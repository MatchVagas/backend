package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.BuscaTalentosRequestDTO;
import com.matchvagas.backend.dto.CandidatoRecomendadoResponseDTO;
import com.matchvagas.backend.dto.PageResponseDTO;
import com.matchvagas.backend.entity.Vagas;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.repository.EmpresaRepository;
import com.matchvagas.backend.service.RecomendacaoCandidatoService.VetorPersistido;
import com.matchvagas.backend.service.embedding.EmbeddingPort;
import com.matchvagas.backend.service.embedding.TextoEmbeddingBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Busca de talentos: a empresa descreve o perfil ideal em texto livre e recebe o ranking
 * dos candidatos com opt-in, sem cadastrar vaga. Usa a mesma pontuação e a mesma
 * minimização de dados da recomendação por vaga ({@link RecomendacaoCandidatoService}).
 */
@Slf4j
@Service
public class BuscaTalentosService {
    static final long JANELA_MS = 60 * 60 * 1000L;

    private final RecomendacaoCandidatoService recomendacaoService;
    private final EmpresaRepository empresaRepository;
    private final EmbeddingPort embeddingPort;
    private final RateLimiterService rateLimiterService;
    private final AuditoriaService auditoriaService;
    private final int limitePorHora;

    public BuscaTalentosService(RecomendacaoCandidatoService recomendacaoService,
                                EmpresaRepository empresaRepository,
                                EmbeddingPort embeddingPort,
                                RateLimiterService rateLimiterService,
                                AuditoriaService auditoriaService,
                                @Value("${app.talentos.limite-por-hora:30}") int limitePorHora) {
        this.recomendacaoService = recomendacaoService;
        this.empresaRepository = empresaRepository;
        this.embeddingPort = embeddingPort;
        this.rateLimiterService = rateLimiterService;
        this.auditoriaService = auditoriaService;
        this.limitePorHora = limitePorHora;
    }

    @Transactional
    public PageResponseDTO<CandidatoRecomendadoResponseDTO> buscar(
            Long usuarioEmpresaId, BuscaTalentosRequestDTO dto, Pageable pageable) {
        if (dto.salarioMinimo() != null && dto.salarioMaximo() != null
                && dto.salarioMinimo().compareTo(dto.salarioMaximo()) > 0) {
            throw new BusinessException("Salário mínimo não pode ser maior que o salário máximo.");
        }
        empresaRepository.findByUsuarioId(usuarioEmpresaId)
                .orElseThrow(() -> new BusinessException("Nenhuma empresa vinculada a este usuário."));
        // Cada página conta como uma busca: o teto impede varrer a base de candidatos.
        rateLimiterService.verificar("talentos:" + usuarioEmpresaId, limitePorHora, JANELA_MS);

        PageResponseDTO<CandidatoRecomendadoResponseDTO> pagina =
                recomendacaoService.ranquear(perfilTextual(dto), vetor(dto), Set.of(), pageable);

        // LGPD art. 37: registra quais candidatos foram expostos a qual empresa.
        pagina.content().forEach(c -> auditoriaService.registrar(
                usuarioEmpresaId, c.candidatoId(), "CANDIDATO", "BUSCA_TALENTOS"));
        return pagina;
    }

    /**
     * Vaga transitória (nunca persistida) usada na pontuação. A comparação textual olha título,
     * área e requisitos; sem eles, a descrição livre faz o papel — senão ninguém passaria do corte.
     */
    private Vagas perfilTextual(BuscaTalentosRequestDTO dto) {
        String descricao = limpo(dto.descricao());
        Vagas perfil = new Vagas();
        perfil.setTitulo(Objects.requireNonNullElse(limpo(dto.titulo()), descricao));
        perfil.setAreaAtuacao(Objects.requireNonNullElse(limpo(dto.areaAtuacao()), descricao));
        perfil.setRequisitos(Stream.of(limpo(dto.requisitos()), descricao)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(". ")));
        perfil.setSalarioMinimo(dto.salarioMinimo());
        perfil.setSalarioMaximo(dto.salarioMaximo());
        return perfil;
    }

    /** null quando os embeddings estão desligados ou falham: o ranking cai na comparação textual. */
    private VetorPersistido vetor(BuscaTalentosRequestDTO dto) {
        if (!embeddingPort.isAtivo()) return null;
        // Mesmo formato do texto indexado das vagas, sem as substituições da comparação textual.
        Vagas perfil = new Vagas();
        perfil.setTitulo(limpo(dto.titulo()));
        perfil.setAreaAtuacao(limpo(dto.areaAtuacao()));
        perfil.setDescricao(limpo(dto.descricao()));
        perfil.setRequisitos(limpo(dto.requisitos()));
        try {
            float[] valores = embeddingPort.embed(TextoEmbeddingBuilder.daVaga(perfil));
            return new VetorPersistido(valores, embeddingPort.modelo(), valores.length);
        } catch (RuntimeException e) {
            log.warn("Falha ao gerar embedding da busca de talentos: {}", e.getMessage());
            return null;
        }
    }

    private static String limpo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }
}
