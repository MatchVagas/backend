package com.matchvagas.backend.service.assistente;

import com.fasterxml.jackson.databind.JsonNode;
import com.matchvagas.backend.dto.TriagemResponseDTO;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Candidatura;
import com.matchvagas.backend.entity.Empresas;
import com.matchvagas.backend.entity.Experiencia;
import com.matchvagas.backend.entity.Formacao;
import com.matchvagas.backend.entity.RecomendacaoTriagem;
import com.matchvagas.backend.entity.TriagemCandidatura;
import com.matchvagas.backend.entity.Vagas;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.repository.CandidaturaRepository;
import com.matchvagas.backend.repository.EmpresaRepository;
import com.matchvagas.backend.repository.ExperienciaRepository;
import com.matchvagas.backend.repository.FormacaoRepository;
import com.matchvagas.backend.repository.TriagemCandidaturaRepository;
import com.matchvagas.backend.service.AuditoriaService;
import com.matchvagas.backend.service.llm.LlmEstruturado;
import com.matchvagas.backend.service.llm.MascaraDadosPessoais;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static com.matchvagas.backend.service.assistente.PromptsAssistivos.NAO_COMPARTILHADO;
import static com.matchvagas.backend.service.assistente.PromptsAssistivos.linha;

/**
 * Triagem inicial assistida: compara o que o candidato compartilhou nesta candidatura
 * com a vaga e sugere um parecer. É só sugestão — nunca move a candidatura. Respeita as
 * mesmas permissões de compartilhamento da visão da empresa (habilidades, contato e
 * endereço nunca entram; o resto, só se o candidato autorizou).
 */
@Service
@RequiredArgsConstructor
public class TriagemCandidaturaService {
    private static final int MAX_ITENS = 5;
    private static final int MAX_TAMANHO_ITEM = 300;
    private static final int MAX_PARECER = 2000;
    private static final int MAX_CAMPO_VAGA = 3000;
    private static final int MAX_DESCRICAO_EXPERIENCIA = 1000;

    private final CandidaturaRepository candidaturaRepository;
    private final EmpresaRepository empresaRepository;
    private final ExperienciaRepository experienciaRepository;
    private final FormacaoRepository formacaoRepository;
    private final TriagemCandidaturaRepository triagemRepository;
    private final LlmEstruturado llm;
    private final CotaAssistente cota;
    private final AuditoriaService auditoriaService;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.llm.max-caracteres-entrada:12000}")
    private int maxCaracteresEntrada;

    private record Contexto(Long candidatoId, String prompt, String hash, TriagemCandidatura existente) {}

    public TriagemResponseDTO buscar(Long candidaturaId, Long usuarioEmpresaId) {
        Contexto ctx = carregar(candidaturaId, usuarioEmpresaId);
        if (ctx.existente() == null) {
            throw new ResourceNotFoundException("Nenhuma triagem gerada para esta candidatura.");
        }
        return dto(ctx.existente(), candidaturaId, !ctx.hash().equals(ctx.existente().getEntradaHash()));
    }

    public TriagemResponseDTO gerar(Long candidaturaId, Long usuarioEmpresaId, boolean regenerar) {
        Contexto ctx = carregar(candidaturaId, usuarioEmpresaId);
        if (!regenerar && ctx.existente() != null && ctx.hash().equals(ctx.existente().getEntradaHash())) {
            return dto(ctx.existente(), candidaturaId, false);
        }
        cota.consumir(usuarioEmpresaId);

        // Fora de transação: em CPU a geração pode levar minutos.
        JsonNode resposta = llm.gerar(PromptsAssistivos.SISTEMA_TRIAGEM, ctx.prompt(), "parecer", "recomendacao");
        String modelo = llm.modelo();

        TriagemCandidatura salva = transactionTemplate.execute(status -> {
            TriagemCandidatura t = triagemRepository.findByCandidaturaId(candidaturaId)
                    .orElseGet(TriagemCandidatura::new);
            t.setCandidatura(candidaturaRepository.getReferenceById(candidaturaId));
            t.setParecer(limitar(LlmEstruturado.texto(resposta.get("parecer")), MAX_PARECER));
            t.setPontosFortes(juntar(itens(resposta.get("pontosFortes"))));
            t.setLacunas(juntar(itens(resposta.get("lacunas"))));
            t.setRecomendacao(recomendacao(LlmEstruturado.texto(resposta.get("recomendacao"))));
            t.setModelo(modelo);
            t.setEntradaHash(ctx.hash());
            t.setGeradoPorUsuarioId(usuarioEmpresaId);
            t.setGeradoEm(LocalDateTime.now());
            TriagemCandidatura gravada = triagemRepository.save(t);
            // LGPD art. 37 — perfil e currículo do candidato foram processados a pedido da empresa.
            auditoriaService.registrar(usuarioEmpresaId, ctx.candidatoId(), "TRIAGEM_IA", "CREATE");
            return gravada;
        });
        return dto(salva, candidaturaId, false);
    }

    private Contexto carregar(Long candidaturaId, Long usuarioEmpresaId) {
        return transactionTemplate.execute(status -> {
            Empresas empresa = empresaRepository.findByUsuarioId(usuarioEmpresaId)
                    .orElseThrow(() -> new BusinessException("Nenhuma empresa vinculada a este usuário."));
            Candidatura candidatura = candidaturaRepository.findById(candidaturaId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Candidatura não encontrada com ID: " + candidaturaId));
            if (!candidatura.getVaga().getEmpresas().getId().equals(empresa.getId())) {
                throw new AccessDeniedException("Esta candidatura não pertence a uma vaga da sua empresa.");
            }
            String prompt = montarPrompt(candidatura);
            return new Contexto(candidatura.getCandidato().getId(), prompt, PromptsAssistivos.hash(prompt),
                    triagemRepository.findByCandidaturaId(candidaturaId).orElse(null));
        });
    }

    private String montarPrompt(Candidatura c) {
        Vagas vaga = c.getVaga();
        Candidatos candidato = c.getCandidato();

        StringBuilder sb = new StringBuilder("<vaga>\n");
        linha(sb, "Título", vaga.getTitulo());
        linha(sb, "Área de atuação", vaga.getAreaAtuacao());
        if (vaga.getTipoVaga() != null) linha(sb, "Tipo de contratação", vaga.getTipoVaga().getDescricao());
        if (vaga.getModalidade() != null) linha(sb, "Modalidade", vaga.getModalidade().getDescricao());
        if (vaga.getEscolaridade() != null) linha(sb, "Escolaridade mínima", vaga.getEscolaridade().getNome());
        linha(sb, "Carga horária", vaga.getCargaHoraria());
        linha(sb, "Faixa salarial", PromptsAssistivos.faixa(vaga.getSalarioMinimo(), vaga.getSalarioMaximo()));
        linha(sb, "Descrição", PromptsAssistivos.truncar(vaga.getDescricao(), MAX_CAMPO_VAGA));
        linha(sb, "Requisitos", PromptsAssistivos.truncar(vaga.getRequisitos(), MAX_CAMPO_VAGA));
        sb.append("</vaga>\n<candidato>\n");

        linha(sb, "Objetivo profissional", c.isCompartilharObjetivoProfissional()
                ? informado(candidato.getObjetivoProfissional()) : NAO_COMPARTILHADO);
        linha(sb, "Disponibilidade", c.isCompartilharDisponibilidade()
                ? informado(candidato.getDisponibilidade()) : NAO_COMPARTILHADO);
        linha(sb, "Pretensão salarial", !c.isCompartilharPretensaoSalarial() ? NAO_COMPARTILHADO
                : candidato.getPretensaoSalarial() == null ? "não informada"
                : PromptsAssistivos.faixa(candidato.getPretensaoSalarial(), candidato.getPretensaoSalarial()));
        linha(sb, "Experiências", c.isCompartilharExperiencias()
                ? experiencias(experienciaRepository.findByCandidatoId(candidato.getId())) : NAO_COMPARTILHADO);
        linha(sb, "Formações", c.isCompartilharFormacoes()
                ? formacoes(formacaoRepository.findByCandidatoId(candidato.getId())) : NAO_COMPARTILHADO);
        linha(sb, "Currículo", c.isCompartilharCurriculo() ? curriculo(candidato) : NAO_COMPARTILHADO);
        sb.append("</candidato>");

        return MascaraDadosPessoais.mascarar(sb.toString());
    }

    private String experiencias(List<Experiencia> lista) {
        if (lista == null || lista.isEmpty()) return "não informadas";
        StringBuilder sb = new StringBuilder();
        for (Experiencia e : lista) {
            sb.append("\n- ").append(informado(e.getCargo()));
            if (e.getEmpresa() != null) sb.append(" em ").append(e.getEmpresa());
            sb.append(periodo(e.getDataInicio(), e.getDataFim()));
            if (e.getDescricao() != null && !e.getDescricao().isBlank()) {
                sb.append(": ").append(PromptsAssistivos.truncar(e.getDescricao().strip(), MAX_DESCRICAO_EXPERIENCIA));
            }
        }
        return sb.toString();
    }

    private String formacoes(List<Formacao> lista) {
        if (lista == null || lista.isEmpty()) return "não informadas";
        StringBuilder sb = new StringBuilder();
        for (Formacao f : lista) {
            sb.append("\n- ").append(informado(f.getCurso()));
            if (f.getNivel() != null) sb.append(" (").append(f.getNivel()).append(')');
            if (f.getInstituicao() != null) sb.append(" — ").append(f.getInstituicao());
            sb.append(periodo(f.getDataInicio(), f.getDataFim()));
        }
        return sb.toString();
    }

    private String curriculo(Candidatos candidato) {
        var cv = candidato.getCurriculo();
        if (cv == null || cv.getTextoExtraido() == null || cv.getTextoExtraido().isBlank()) return "não informado";
        return "\n" + PromptsAssistivos.truncar(cv.getTextoExtraido(), maxCaracteresEntrada);
    }

    private static String periodo(String inicio, String fim) {
        if (inicio == null && fim == null) return "";
        return " (" + (inicio == null ? "?" : inicio) + " – " + (fim == null ? "atual" : fim) + ")";
    }

    private static String informado(String valor) {
        return valor == null || valor.isBlank() ? "não informado" : valor.strip();
    }

    private static List<String> itens(JsonNode no) {
        return LlmEstruturado.lista(no).stream().limit(MAX_ITENS).map(i -> limitar(i, MAX_TAMANHO_ITEM)).toList();
    }

    private static String limitar(String texto, int max) {
        return texto == null || texto.length() <= max ? texto : texto.substring(0, max).strip();
    }

    // Itens já vêm sem quebras de linha (LlmEstruturado.lista normaliza os espaços).
    private static String juntar(List<String> itens) {
        return itens.isEmpty() ? null : String.join("\n", itens);
    }

    private static List<String> separar(String texto) {
        return texto == null || texto.isBlank() ? List.of() : Arrays.asList(texto.split("\n"));
    }

    static RecomendacaoTriagem recomendacao(String valor) {
        if (valor == null) return RecomendacaoTriagem.REVISAR;
        String chave = PromptsAssistivos.chave(valor).replaceAll("[\\s-]+", "_").toUpperCase();
        try {
            return RecomendacaoTriagem.valueOf(chave);
        } catch (IllegalArgumentException e) {
            return RecomendacaoTriagem.REVISAR; // valor fora do combinado: o recrutador decide
        }
    }

    private static TriagemResponseDTO dto(TriagemCandidatura t, Long candidaturaId, boolean desatualizado) {
        return new TriagemResponseDTO(candidaturaId, t.getParecer(), separar(t.getPontosFortes()),
                separar(t.getLacunas()), t.getRecomendacao(), t.getModelo(), t.getGeradoEm(),
                desatualizado, TriagemResponseDTO.AVISO);
    }
}
