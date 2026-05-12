package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.SugestaoVagaResponseDTO;
import com.matchvagas.backend.dto.VagaResponseDTO;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Vagas;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.VagasMapper;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.CandidaturaRepository;
import com.matchvagas.backend.repository.VagaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SugestaoVagaService {

    private final CandidatoRepository    candidatoRepository;
    private final VagaRepository         vagaRepository;
    private final CandidaturaRepository  candidaturaRepository;
    private final VagasMapper            vagasMapper;

    private static final int MAX_SUGESTOES = 10;

    // Palavras sem valor semântico para filtragem de texto.
    // Inclui preposições, artigos, conjunções E descritores de nível de cargo
    // (senior, junior, etc.) que causam falsos positivos entre áreas distintas.
    private static final Set<String> STOPWORDS = Set.of(
            // Preposições, artigos e conjunções
            "de", "da", "do", "das", "dos", "em", "no", "na", "nos", "nas",
            "com", "para", "por", "pelo", "pela", "pelos", "pelas",
            "um", "uma", "uns", "umas", "a", "o", "as", "os",
            "e", "ou", "se", "que", "ao", "aos", "ser", "ter", "sua",
            "seu", "seus", "suas", "mais", "mas", "como", "isso",
            // Descritores de nível de cargo (não indicam área de atuação)
            "senior", "junior", "júnior", "pleno", "trainee", "estagio",
            "estagiario", "coordenador", "supervisor", "gerente", "diretor",
            "chefe", "lider", "especialista", "consultor", "assistente"
    );

    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SugestaoVagaResponseDTO> sugerirVagas(Long usuarioId) {
        Candidatos candidato = candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de candidato não encontrado. Complete seu cadastro para receber sugestões."));

        // Vagas às quais o candidato já se candidatou (excluídas das sugestões)
        Set<Long> jaAplicadas = candidaturaRepository
                .findByCandidatoId(candidato.getId())
                .stream()
                .map(c -> c.getVaga().getId())
                .collect(Collectors.toSet());

        // Apenas vagas ativas e dentro do prazo de validade
        List<Vagas> candidatas = vagaRepository
                .findVagasAtivas(LocalDateTime.now())
                .stream()
                .filter(v -> !jaAplicadas.contains(v.getId()))
                .collect(Collectors.toList());

        return candidatas.stream()
                .map(v -> pontuar(v, candidato))
                .filter(s -> s.pontuacao() > 15) // age-only match (15 pts) não é suficiente
                .sorted(Comparator.comparingInt(SugestaoVagaResponseDTO::pontuacao).reversed())
                .limit(MAX_SUGESTOES)
                .collect(Collectors.toList());
    }

    // ── Algoritmo de pontuação (100 pts máx) ─────────────────────────────────

    private SugestaoVagaResponseDTO pontuar(Vagas vaga, Candidatos candidato) {
        int pontuacao = 0;
        List<String> motivos = new ArrayList<>();

        String objetivo = candidato.getObjetivoProfissional();

        // ── 1. Área de atuação × objetivo profissional (30 pts) ───────────────
        if (objetivo != null && vaga.getAreaAtuacao() != null) {
            if (textosSimilares(objetivo, vaga.getAreaAtuacao())) {
                pontuacao += 30;
                motivos.add("Área de atuação compatível com seu objetivo profissional");
            }
        }

        // ── 2. Palavras-chave no título da vaga (15 pts) ──────────────────────
        if (objetivo != null && vaga.getTitulo() != null) {
            if (textosSimilares(objetivo, vaga.getTitulo())) {
                pontuacao += 15;
                motivos.add("Seu perfil se encaixa no título da vaga");
            }
        }

        // ── 3. Palavras-chave nos requisitos (15 pts) ─────────────────────────
        if (objetivo != null && vaga.getRequisitos() != null) {
            if (textosSimilares(objetivo, vaga.getRequisitos())) {
                pontuacao += 15;
                motivos.add("Seu perfil corresponde aos requisitos exigidos");
            }
        }

        // ── 4. Compatibilidade salarial (25 pts / 10 pts) ────────────────────
        BigDecimal pretensao = candidato.getPretensaoSalarial();
        if (pretensao != null && vaga.getSalarioMinimo() != null && vaga.getSalarioMaximo() != null) {
            int cmpMin = pretensao.compareTo(vaga.getSalarioMinimo());
            int cmpMax = pretensao.compareTo(vaga.getSalarioMaximo());

            if (cmpMin >= 0 && cmpMax <= 0) {
                // Pretensão dentro da faixa salarial
                pontuacao += 25;
                motivos.add("Faixa salarial compatível com sua pretensão ("
                        + formatarSalario(vaga.getSalarioMinimo())
                        + " – " + formatarSalario(vaga.getSalarioMaximo()) + ")");
            } else if (cmpMax > 0) {
                // Pretensão acima do teto — empresa paga menos do que o candidato quer
                // (sem bônus)
            } else {
                // Pretensão abaixo do piso — empresa paga mais do que o candidato pede
                pontuacao += 10;
                motivos.add("Salário oferecido acima da sua pretensão ("
                        + formatarSalario(vaga.getSalarioMinimo()) + "+)");
            }
        }

        // ── 5. Faixa etária (15 pts) ──────────────────────────────────────────
        Integer idadeCandidato = (candidato.getUsuario() != null)
                ? candidato.getUsuario().getIdade() : null;

        if (idadeCandidato != null && idadeCandidato > 0
                && vaga.getIdadeMinima() > 0 && vaga.getIdadeMaxima() > 0) {
            if (idadeCandidato >= vaga.getIdadeMinima()
                    && idadeCandidato <= vaga.getIdadeMaxima()) {
                pontuacao += 15;
                motivos.add("Sua faixa etária é compatível com a vaga ("
                        + vaga.getIdadeMinima() + "–" + vaga.getIdadeMaxima() + " anos)");
            }
        }

        VagaResponseDTO vagaDTO = vagasMapper.toDTO(vaga);
        return new SugestaoVagaResponseDTO(vagaDTO, pontuacao, motivos);
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    /**
     * Retorna true se os dois textos compartilham pelo menos uma palavra
     * significativa (tamanho > 2, fora das stopwords).
     */
    private boolean textosSimilares(String a, String b) {
        Set<String> palavrasA = extrairPalavras(a);
        Set<String> palavrasB = extrairPalavras(b);
        return palavrasA.stream().anyMatch(palavrasB::contains);
    }

    private Set<String> extrairPalavras(String texto) {
        if (texto == null || texto.isBlank()) return Collections.emptySet();
        return Arrays.stream(
                        texto.toLowerCase()
                                .replaceAll("[^a-záéíóúâêîôûãõç ]", " ")
                                .split("\\s+"))
                .filter(p -> p.length() > 2 && !STOPWORDS.contains(p))
                .collect(Collectors.toSet());
    }

    private String formatarSalario(BigDecimal valor) {
        if (valor == null) return "–";
        return "R$ " + String.format("%,.0f", valor);
    }
}
