package com.matchvagas.backend.service.assistente;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchvagas.backend.dto.ResumoPerfilResponseDTO;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Curriculos;
import com.matchvagas.backend.entity.Habilidade;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.CurriculoRepository;
import com.matchvagas.backend.service.llm.LlmEstruturado;
import com.matchvagas.backend.service.llm.MascaraDadosPessoais;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sugere objetivo profissional e habilidades a partir do currículo. Nunca altera o
 * perfil — o candidato revisa e aplica. A sugestão fica salva no currículo e é
 * reaproveitada enquanto o texto do currículo não mudar.
 */
@Service
@RequiredArgsConstructor
public class ResumoPerfilService {
    private static final int MAX_HABILIDADES = 15;
    private static final int MAX_TAMANHO_HABILIDADE = 100; // coluna habilidade.nome
    private static final int MAX_OBJETIVO = 1000;

    private final CandidatoRepository candidatoRepository;
    private final CurriculoRepository curriculoRepository;
    private final LlmEstruturado llm;
    private final CotaAssistente cota;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.llm.max-caracteres-entrada:12000}")
    private int maxCaracteresEntrada;

    /** Resposta normalizada do LLM; é o que fica salvo (JSON) no currículo. */
    record Sugestao(String objetivoProfissional, List<String> habilidades) {}

    private record Contexto(Long curriculoId, String prompt, String hash, Set<String> habilidadesAtuais,
                            Sugestao salva, String hashSalvo, String modeloSalvo, LocalDateTime geradoEm) {}

    public ResumoPerfilResponseDTO buscar(Long usuarioId) {
        Contexto ctx = carregar(usuarioId);
        if (ctx.salva() == null) {
            throw new ResourceNotFoundException("Nenhuma sugestão gerada para o currículo atual.");
        }
        return dto(ctx.salva(), ctx.modeloSalvo(), ctx.geradoEm(),
                !ctx.hash().equals(ctx.hashSalvo()), ctx.habilidadesAtuais());
    }

    public ResumoPerfilResponseDTO gerar(Long usuarioId, boolean regenerar) {
        Contexto ctx = carregar(usuarioId);
        if (!regenerar && ctx.salva() != null && ctx.hash().equals(ctx.hashSalvo())) {
            return dto(ctx.salva(), ctx.modeloSalvo(), ctx.geradoEm(), false, ctx.habilidadesAtuais());
        }
        cota.consumir(usuarioId);

        // Fora de transação: em CPU a geração pode levar minutos.
        JsonNode resposta = llm.gerar(PromptsAssistivos.SISTEMA_RESUMO, ctx.prompt(), "objetivoProfissional");
        Sugestao sugestao = normalizar(resposta);
        String modelo = llm.modelo();
        LocalDateTime geradoEm = LocalDateTime.now();

        transactionTemplate.executeWithoutResult(status -> {
            Curriculos curriculo = curriculoRepository.findById(ctx.curriculoId())
                    .orElseThrow(() -> new ResourceNotFoundException("O currículo foi removido durante a geração."));
            curriculo.setResumoIa(escrever(sugestao));
            curriculo.setResumoIaHash(ctx.hash());
            curriculo.setResumoIaModelo(modelo);
            curriculo.setResumoIaGeradoEm(geradoEm);
            curriculoRepository.save(curriculo);
        });
        return dto(sugestao, modelo, geradoEm, false, ctx.habilidadesAtuais());
    }

    private Contexto carregar(Long usuarioId) {
        return transactionTemplate.execute(status -> {
            Candidatos candidato = candidatoRepository.findByUsuarioId(usuarioId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Perfil de candidato não encontrado para o usuário ID: " + usuarioId));
            Curriculos curriculo = curriculoRepository.findByCandidatoId(candidato.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Nenhum currículo cadastrado."));
            if (curriculo.getTextoExtraido() == null || curriculo.getTextoExtraido().isBlank()) {
                throw new BusinessException(
                        "O texto deste currículo não foi extraído. Envie o arquivo novamente para usar o assistente.");
            }
            String prompt = PromptsAssistivos.usuarioResumo(PromptsAssistivos.truncar(
                    MascaraDadosPessoais.mascarar(curriculo.getTextoExtraido()), maxCaracteresEntrada));
            Set<String> atuais = candidato.getHabilidades() == null ? Set.of() : candidato.getHabilidades().stream()
                    .map(Habilidade::getNome).filter(Objects::nonNull)
                    .map(PromptsAssistivos::chave).collect(Collectors.toSet());
            return new Contexto(curriculo.getId(), prompt, PromptsAssistivos.hash(prompt), atuais,
                    ler(curriculo.getResumoIa()), curriculo.getResumoIaHash(),
                    curriculo.getResumoIaModelo(), curriculo.getResumoIaGeradoEm());
        });
    }

    private Sugestao normalizar(JsonNode resposta) {
        String objetivo = LlmEstruturado.texto(resposta.get("objetivoProfissional"));
        if (objetivo.length() > MAX_OBJETIVO) objetivo = objetivo.substring(0, MAX_OBJETIVO).strip();

        // Modelos pequenos às vezes devolvem "Java, Spring, SQL" num item só.
        Map<String, String> unicas = new LinkedHashMap<>();
        for (String item : LlmEstruturado.lista(resposta.get("habilidades"))) {
            for (String parte : item.split(",")) {
                String habilidade = parte.strip();
                if (!habilidade.isEmpty() && habilidade.length() <= MAX_TAMANHO_HABILIDADE) {
                    unicas.putIfAbsent(PromptsAssistivos.chave(habilidade), habilidade);
                }
            }
        }
        return new Sugestao(objetivo, unicas.values().stream().limit(MAX_HABILIDADES).toList());
    }

    private ResumoPerfilResponseDTO dto(Sugestao s, String modelo, LocalDateTime geradoEm,
                                        boolean desatualizado, Set<String> habilidadesAtuais) {
        // Só sugere o que o candidato ainda não tem no perfil.
        List<String> novas = s.habilidades() == null ? List.of() : s.habilidades().stream()
                .filter(h -> !habilidadesAtuais.contains(PromptsAssistivos.chave(h)))
                .toList();
        return new ResumoPerfilResponseDTO(s.objetivoProfissional(), novas, modelo, geradoEm, desatualizado);
    }

    private String escrever(Sugestao sugestao) {
        try {
            return objectMapper.writeValueAsString(sugestao);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar a sugestão do assistente.", e);
        }
    }

    private Sugestao ler(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Sugestao.class);
        } catch (JsonProcessingException e) {
            return null; // registro corrompido: trata como inexistente e deixa gerar de novo
        }
    }
}
