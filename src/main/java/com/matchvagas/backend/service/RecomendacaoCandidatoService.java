package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.CandidatoRecomendadoResponseDTO;
import com.matchvagas.backend.dto.PageResponseDTO;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Vagas;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.repository.CandidatoEmbeddingRepository;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.CandidaturaRepository;
import com.matchvagas.backend.repository.EmpresaRepository;
import com.matchvagas.backend.repository.VagaEmbeddingRepository;
import com.matchvagas.backend.repository.VagaRepository;
import com.matchvagas.backend.service.embedding.EmbeddingCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecomendacaoCandidatoService {
    private static final int PTS_SEMANTICO_MAX = 60;
    private static final Set<String> STOPWORDS = Set.of(
            "de", "da", "do", "das", "dos", "em", "no", "na", "nos", "nas",
            "com", "para", "por", "pelo", "pela", "pelos", "pelas", "um", "uma",
            "uns", "umas", "a", "o", "as", "os", "e", "ou", "se", "que", "ao",
            "aos", "ser", "ter", "sua", "seu", "seus", "suas", "mais", "mas", "como",
            "senior", "junior", "júnior", "pleno", "trainee", "estagio", "estagiario",
            "coordenador", "supervisor", "gerente", "diretor", "chefe", "lider", "especialista",
            "consultor", "assistente");

    private final VagaRepository vagaRepository;
    private final EmpresaRepository empresaRepository;
    private final CandidatoRepository candidatoRepository;
    private final CandidaturaRepository candidaturaRepository;
    private final VagaEmbeddingRepository vagaEmbeddingRepository;
    private final CandidatoEmbeddingRepository candidatoEmbeddingRepository;

    @Value("${app.embeddings.limiar:0.30}")
    private double limiarSemantico;

    @Value("${app.embeddings.enabled:false}")
    private boolean embeddingsAtivos;

    private record VetorPersistido(float[] valores, String modelo, int dimensao) {}

    @Transactional(readOnly = true)
    public PageResponseDTO<CandidatoRecomendadoResponseDTO> recomendar(
            Long vagaId, Long usuarioEmpresaId, Pageable pageable) {
        Vagas vaga = vagaRepository.findById(vagaId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaga não encontrada com ID: " + vagaId));
        var empresa = empresaRepository.findByUsuarioId(usuarioEmpresaId)
                .orElseThrow(() -> new BusinessException("Nenhuma empresa vinculada a este usuário."));
        if (vaga.getEmpresas() == null || !Objects.equals(vaga.getEmpresas().getId(), empresa.getId())) {
            throw new BusinessException("Você não tem permissão para consultar recomendações desta vaga.");
        }

        Set<Long> jaCandidatados = candidaturaRepository.findByVagaId(vagaId).stream()
                .map(c -> c.getCandidato().getId())
                .collect(Collectors.toSet());
        List<Candidatos> candidatos = candidatoRepository.findByDisponivelParaRecomendacoesTrue().stream()
                .filter(c -> !jaCandidatados.contains(c.getId()))
                .filter(c -> c.getUsuario() != null && Boolean.TRUE.equals(c.getUsuario().getAtivo()))
                .toList();

        VetorPersistido vetorVaga = null;
        Map<Long, VetorPersistido> vetoresCandidatos = Collections.emptyMap();
        if (embeddingsAtivos) {
            vetorVaga = vagaEmbeddingRepository.findByVagaId(vagaId)
                    .map(e -> vetor(e.getVetor(), e.getModelo(), e.getDim()))
                    .orElse(null);
            List<Long> ids = candidatos.stream().map(Candidatos::getId).toList();
            if (!ids.isEmpty()) {
                vetoresCandidatos = candidatoEmbeddingRepository.findByCandidatoIdIn(ids).stream()
                        .collect(Collectors.toMap(e -> e.getCandidato().getId(),
                                e -> vetor(e.getVetor(), e.getModelo(), e.getDim()), (a, b) -> a));
            }
        }

        VetorPersistido vetorVagaFinal = vetorVaga;
        Map<Long, VetorPersistido> vetoresFinal = vetoresCandidatos;
        List<CandidatoRecomendadoResponseDTO> ranking = candidatos.stream()
                .map(c -> pontuar(vaga, c, vetorVagaFinal, vetoresFinal.get(c.getId())))
                .filter(c -> c.pontuacao() > 15)
                .sorted(Comparator.comparingInt(CandidatoRecomendadoResponseDTO::pontuacao).reversed()
                        .thenComparing(CandidatoRecomendadoResponseDTO::candidatoId))
                .toList();

        int inicio = Math.min((int) pageable.getOffset(), ranking.size());
        int fim = Math.min(inicio + pageable.getPageSize(), ranking.size());
        int totalPaginas = ranking.isEmpty() ? 0
                : (int) Math.ceil((double) ranking.size() / pageable.getPageSize());
        return new PageResponseDTO<>(ranking.subList(inicio, fim), pageable.getPageNumber(),
                pageable.getPageSize(), ranking.size(), totalPaginas,
                pageable.getPageNumber() >= Math.max(0, totalPaginas - 1));
    }

    private CandidatoRecomendadoResponseDTO pontuar(
            Vagas vaga, Candidatos candidato, VetorPersistido vagaVetor, VetorPersistido candidatoVetor) {
        int pontos = 0;
        List<String> motivos = new ArrayList<>();
        Integer semantico = pontuarSemantico(candidatoVetor, vagaVetor, motivos);
        if (semantico != null) {
            pontos += semantico;
        } else {
            String perfil = textoProfissional(candidato);
            if (textosSimilares(perfil, vaga.getAreaAtuacao())) {
                pontos += 30;
                motivos.add("Área profissional compatível com a vaga");
            }
            if (textosSimilares(perfil, vaga.getTitulo())) {
                pontos += 15;
                motivos.add("Perfil compatível com o título da vaga");
            }
            if (textosSimilares(perfil, vaga.getRequisitos())) {
                pontos += 15;
                motivos.add("Competências compatíveis com os requisitos");
            }
        }

        BigDecimal pretensao = candidato.getPretensaoSalarial();
        if (pretensao != null && vaga.getSalarioMinimo() != null && vaga.getSalarioMaximo() != null) {
            if (pretensao.compareTo(vaga.getSalarioMinimo()) >= 0
                    && pretensao.compareTo(vaga.getSalarioMaximo()) <= 0) {
                pontos += 25;
                motivos.add("Pretensão salarial dentro da faixa da vaga");
            } else if (pretensao.compareTo(vaga.getSalarioMinimo()) < 0) {
                pontos += 10;
                motivos.add("Pretensão salarial abaixo da faixa oferecida");
            }
        }

        Integer idade = candidato.getUsuario() == null ? null : candidato.getUsuario().getIdade();
        if (idade != null && vaga.getIdadeMinima() != null && vaga.getIdadeMaxima() != null
                && vaga.getIdadeMinima() > 0 && vaga.getIdadeMaxima() > 0
                && idade >= vaga.getIdadeMinima() && idade <= vaga.getIdadeMaxima()) {
            pontos += 15;
            motivos.add("Faixa etária compatível com a vaga");
        }

        List<String> habilidades = candidato.getHabilidades() == null ? List.of()
                : candidato.getHabilidades().stream().map(h -> h.getNome())
                        .filter(Objects::nonNull).sorted().toList();
        return new CandidatoRecomendadoResponseDTO(candidato.getId(), candidato.getObjetivoProfissional(),
                candidato.getDisponibilidade(), candidato.getPretensaoSalarial(), habilidades,
                Math.min(100, pontos), List.copyOf(motivos));
    }

    private Integer pontuarSemantico(VetorPersistido candidato, VetorPersistido vaga, List<String> motivos) {
        if (candidato == null || vaga == null || candidato.dimensao() != vaga.dimensao()
                || !Objects.equals(candidato.modelo(), vaga.modelo())
                || candidato.valores().length != candidato.dimensao()
                || vaga.valores().length != vaga.dimensao()) return null;
        double similaridade = Math.max(0, Math.min(1,
                EmbeddingCodec.cosseno(candidato.valores(), vaga.valores())));
        if (similaridade < limiarSemantico) return 0;
        motivos.add(String.format(Locale.forLanguageTag("pt-BR"),
                "Compatibilidade semântica com a vaga (%.0f%%)", similaridade * 100));
        return (int) Math.round(similaridade * PTS_SEMANTICO_MAX);
    }

    private VetorPersistido vetor(String csv, String modelo, int dimensao) {
        return new VetorPersistido(EmbeddingCodec.fromCsv(csv), modelo, dimensao);
    }

    private String textoProfissional(Candidatos candidato) {
        String habilidades = candidato.getHabilidades() == null ? "" : candidato.getHabilidades().stream()
                .map(h -> h.getNome()).filter(Objects::nonNull).collect(Collectors.joining(" "));
        return String.join(" ", Objects.toString(candidato.getObjetivoProfissional(), ""), habilidades);
    }

    private boolean textosSimilares(String a, String b) {
        Set<String> palavrasA = extrairPalavras(a);
        Set<String> palavrasB = extrairPalavras(b);
        return palavrasA.stream().anyMatch(palavrasB::contains);
    }

    private Set<String> extrairPalavras(String texto) {
        if (texto == null || texto.isBlank()) return Collections.emptySet();
        return Arrays.stream(texto.toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-záéíóúâêîôûãõç ]", " ").split("\\s+"))
                .filter(p -> p.length() > 2 && !STOPWORDS.contains(p))
                .collect(Collectors.toSet());
    }
}
