package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.BuscaTalentosRequestDTO;
import com.matchvagas.backend.entity.CandidatoEmbedding;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Empresas;
import com.matchvagas.backend.entity.Habilidade;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.RateLimitException;
import com.matchvagas.backend.repository.CandidatoEmbeddingRepository;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.CandidaturaRepository;
import com.matchvagas.backend.repository.EmpresaRepository;
import com.matchvagas.backend.repository.VagaEmbeddingRepository;
import com.matchvagas.backend.repository.VagaRepository;
import com.matchvagas.backend.service.embedding.EmbeddingPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fase 3 — busca de talentos sem vaga cadastrada")
class BuscaTalentosServiceTest {
    @Mock VagaRepository vagaRepository;
    @Mock EmpresaRepository empresaRepository;
    @Mock CandidatoRepository candidatoRepository;
    @Mock CandidaturaRepository candidaturaRepository;
    @Mock VagaEmbeddingRepository vagaEmbeddingRepository;
    @Mock CandidatoEmbeddingRepository candidatoEmbeddingRepository;
    @Mock EmbeddingPort embeddingPort;
    @Mock RateLimiterService rateLimiterService;
    @Mock AuditoriaService auditoriaService;

    private BuscaTalentosService service;

    @BeforeEach
    void setUp() {
        RecomendacaoCandidatoService recomendacao = new RecomendacaoCandidatoService(vagaRepository,
                empresaRepository, candidatoRepository, candidaturaRepository, vagaEmbeddingRepository,
                candidatoEmbeddingRepository);
        ReflectionTestUtils.setField(recomendacao, "limiarSemantico", 0.30);
        service = new BuscaTalentosService(recomendacao, empresaRepository, embeddingPort,
                rateLimiterService, auditoriaService, 30);
    }

    @Test
    void encontraCandidatoComOptInPorTextoQuandoEmbeddingsDesligados() {
        when(empresaRepository.findByUsuarioId(5L)).thenReturn(Optional.of(new Empresas()));
        when(embeddingPort.isAtivo()).thenReturn(false);
        when(candidatoRepository.findByDisponivelParaRecomendacoesTrue())
                .thenReturn(List.of(candidato(30L, "Desenvolvedor Java backend")));

        var pagina = service.buscar(5L, busca("Procuro alguém com Java e Spring para backend"),
                PageRequest.of(0, 20));

        assertThat(pagina.totalElements()).isEqualTo(1);
        assertThat(pagina.content().getFirst().candidatoId()).isEqualTo(30L);
        assertThat(pagina.content().getFirst().pontuacao()).isEqualTo(60);
        assertThat(pagina.content().getFirst().motivos()).contains("Competências compatíveis com os requisitos");
        verify(rateLimiterService).verificar(eq("talentos:5"), eq(30), anyLong());
        verify(auditoriaService).registrar(5L, 30L, "CANDIDATO", "BUSCA_TALENTOS");
        verifyNoInteractions(candidatoEmbeddingRepository);
    }

    @Test
    void usaSimilaridadeSemanticaComVetorGeradoParaABusca() {
        when(empresaRepository.findByUsuarioId(5L)).thenReturn(Optional.of(new Empresas()));
        when(embeddingPort.isAtivo()).thenReturn(true);
        when(embeddingPort.embed(anyString())).thenReturn(new float[]{1f, 0f});
        when(embeddingPort.modelo()).thenReturn("modelo");
        Candidatos candidato = candidato(30L, "Outra área");
        when(candidatoRepository.findByDisponivelParaRecomendacoesTrue()).thenReturn(List.of(candidato));
        CandidatoEmbedding embedding = new CandidatoEmbedding();
        embedding.setCandidato(candidato);
        embedding.setVetor("1.0,0.0");
        embedding.setModelo("modelo");
        embedding.setDim(2);
        when(candidatoEmbeddingRepository.findByCandidatoIdIn(List.of(30L))).thenReturn(List.of(embedding));

        var item = service.buscar(5L, busca("Engenheiro de dados"), PageRequest.of(0, 20)).content().getFirst();

        assertThat(item.pontuacao()).isEqualTo(60);
        assertThat(item.motivos()).anyMatch(m -> m.contains("100%"));
    }

    @Test
    void caiNaComparacaoTextualQuandoOEmbeddingFalha() {
        when(empresaRepository.findByUsuarioId(5L)).thenReturn(Optional.of(new Empresas()));
        when(embeddingPort.isAtivo()).thenReturn(true);
        when(embeddingPort.embed(anyString())).thenThrow(new IllegalStateException("modelo indisponível"));
        when(candidatoRepository.findByDisponivelParaRecomendacoesTrue())
                .thenReturn(List.of(candidato(30L, "Desenvolvedor Java")));

        var pagina = service.buscar(5L, busca("Java"), PageRequest.of(0, 20));

        assertThat(pagina.totalElements()).isEqualTo(1);
        verify(candidatoEmbeddingRepository, never()).findByCandidatoIdIn(anyCollection());
    }

    @Test
    void ignoraCandidatoInativo() {
        when(empresaRepository.findByUsuarioId(5L)).thenReturn(Optional.of(new Empresas()));
        when(embeddingPort.isAtivo()).thenReturn(false);
        Candidatos inativo = candidato(30L, "Desenvolvedor Java");
        inativo.getUsuario().setAtivo(false);
        when(candidatoRepository.findByDisponivelParaRecomendacoesTrue()).thenReturn(List.of(inativo));

        assertThat(service.buscar(5L, busca("Java"), PageRequest.of(0, 20)).totalElements()).isZero();
        verifyNoInteractions(auditoriaService);
    }

    @Test
    void exigeEmpresaVinculadaAntesDeConsumirCota() {
        when(empresaRepository.findByUsuarioId(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(5L, busca("Java"), PageRequest.of(0, 20)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("empresa");
        verifyNoInteractions(rateLimiterService, candidatoRepository);
    }

    @Test
    void respeitaACotaPorHora() {
        when(empresaRepository.findByUsuarioId(5L)).thenReturn(Optional.of(new Empresas()));
        doThrow(new RateLimitException("Muitas tentativas."))
                .when(rateLimiterService).verificar(anyString(), anyInt(), anyLong());

        assertThatThrownBy(() -> service.buscar(5L, busca("Java"), PageRequest.of(0, 20)))
                .isInstanceOf(RateLimitException.class);
        verifyNoInteractions(candidatoRepository);
    }

    @Test
    void rejeitaFaixaSalarialInvertida() {
        var dto = new BuscaTalentosRequestDTO("Java", null, null, null,
                new BigDecimal("9000"), new BigDecimal("5000"));

        assertThatThrownBy(() -> service.buscar(5L, dto, PageRequest.of(0, 20)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Salário");
    }

    private BuscaTalentosRequestDTO busca(String descricao) {
        return new BuscaTalentosRequestDTO(descricao, null, null, null, null, null);
    }

    private Candidatos candidato(Long id, String objetivo) {
        Usuarios usuario = new Usuarios();
        usuario.setAtivo(true);
        Habilidade habilidade = new Habilidade();
        habilidade.setNome("Java");
        Candidatos candidato = new Candidatos();
        candidato.setId(id);
        candidato.setUsuario(usuario);
        candidato.setObjetivoProfissional(objetivo);
        candidato.setHabilidades(List.of(habilidade));
        candidato.setDisponivelParaRecomendacoes(true);
        return candidato;
    }
}
