package com.matchvagas.backend.service.embedding;

import com.matchvagas.backend.entity.CandidatoEmbedding;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.VagaEmbedding;
import com.matchvagas.backend.entity.Vagas;
import com.matchvagas.backend.repository.CandidatoEmbeddingRepository;
import com.matchvagas.backend.repository.ExperienciaRepository;
import com.matchvagas.backend.repository.FormacaoRepository;
import com.matchvagas.backend.repository.VagaEmbeddingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fase 3 — indexação de embeddings")
class IndexacaoEmbeddingServiceTest {

    @Mock EmbeddingPort embeddingPort;
    @Mock VagaEmbeddingRepository vagaEmbeddingRepository;
    @Mock CandidatoEmbeddingRepository candidatoEmbeddingRepository;
    @Mock ExperienciaRepository experienciaRepository;
    @Mock FormacaoRepository formacaoRepository;
    @InjectMocks IndexacaoEmbeddingService service;

    private Vagas vaga() {
        Vagas vaga = new Vagas();
        vaga.setId(1L);
        vaga.setTitulo("Dev Java");
        return vaga;
    }

    @Test
    @DisplayName("indexa a vaga gravando vetor, dimensão, modelo e hash do texto")
    void indexaVaga() {
        when(embeddingPort.isAtivo()).thenReturn(true);
        when(embeddingPort.modelo()).thenReturn("e5");
        when(embeddingPort.embed("passage: Dev Java")).thenReturn(new float[]{0.5f, 0.5f});
        when(vagaEmbeddingRepository.findByVagaId(1L)).thenReturn(Optional.empty());

        assertThat(service.indexarVaga(vaga())).isTrue();

        ArgumentCaptor<VagaEmbedding> captor = ArgumentCaptor.forClass(VagaEmbedding.class);
        verify(vagaEmbeddingRepository).save(captor.capture());
        assertThat(captor.getValue().getVetor()).isEqualTo("0.5,0.5");
        assertThat(captor.getValue().getDim()).isEqualTo(2);
        assertThat(captor.getValue().getModelo()).isEqualTo("e5");
        assertThat(captor.getValue().getTextoHash())
                .isEqualTo(IndexacaoEmbeddingService.sha256("passage: Dev Java"));
    }

    @Test
    @DisplayName("não recalcula quando texto e modelo não mudaram")
    void naoRecalculaSemMudanca() {
        VagaEmbedding existente = new VagaEmbedding();
        existente.setTextoHash(IndexacaoEmbeddingService.sha256("passage: Dev Java"));
        existente.setModelo("e5");
        when(embeddingPort.isAtivo()).thenReturn(true);
        when(embeddingPort.modelo()).thenReturn("e5");
        when(vagaEmbeddingRepository.findByVagaId(1L)).thenReturn(Optional.of(existente));

        assertThat(service.indexarVaga(vaga())).isFalse();
        verify(embeddingPort, never()).embed(anyString());
        verify(vagaEmbeddingRepository, never()).save(any());
    }

    @Test
    @DisplayName("recalcula quando o modelo mudou, mesmo com o mesmo texto")
    void recalculaQuandoModeloMuda() {
        VagaEmbedding existente = new VagaEmbedding();
        existente.setTextoHash(IndexacaoEmbeddingService.sha256("passage: Dev Java"));
        existente.setModelo("modelo-antigo");
        when(embeddingPort.isAtivo()).thenReturn(true);
        when(embeddingPort.modelo()).thenReturn("e5");
        when(embeddingPort.embed(anyString())).thenReturn(new float[]{1f});
        when(vagaEmbeddingRepository.findByVagaId(1L)).thenReturn(Optional.of(existente));

        assertThat(service.indexarVaga(vaga())).isTrue();
        verify(vagaEmbeddingRepository).save(existente);
    }

    @Test
    @DisplayName("desligado, ou vaga sem id, não faz nada")
    void desligadoOuSemId() {
        when(embeddingPort.isAtivo()).thenReturn(false);
        assertThat(service.indexarVaga(vaga())).isFalse();

        when(embeddingPort.isAtivo()).thenReturn(true);
        assertThat(service.indexarVaga(new Vagas())).isFalse();
        assertThat(service.indexarVaga(null)).isFalse();
        verifyNoInteractions(vagaEmbeddingRepository);
    }

    @Test
    @DisplayName("falha do modelo não propaga: devolve false")
    void falhaDoModelo() {
        when(embeddingPort.isAtivo()).thenReturn(true);
        when(vagaEmbeddingRepository.findByVagaId(1L)).thenReturn(Optional.empty());
        when(embeddingPort.embed(anyString())).thenThrow(new IllegalStateException("sem modelo"));

        assertThat(service.indexarVaga(vaga())).isFalse();
        verify(vagaEmbeddingRepository, never()).save(any());
    }

    @Test
    @DisplayName("indexa o candidato com experiências e formações")
    void indexaCandidato() {
        Candidatos candidato = new Candidatos();
        candidato.setId(7L);
        candidato.setObjetivoProfissional("Backend");
        when(embeddingPort.isAtivo()).thenReturn(true);
        when(embeddingPort.modelo()).thenReturn("e5");
        when(experienciaRepository.findByCandidatoId(7L)).thenReturn(List.of());
        when(formacaoRepository.findByCandidatoId(7L)).thenReturn(List.of());
        when(candidatoEmbeddingRepository.findByCandidatoId(7L)).thenReturn(Optional.empty());
        when(embeddingPort.embed("query: Backend")).thenReturn(new float[]{1f, 0f});

        assertThat(service.indexarCandidato(candidato)).isTrue();

        ArgumentCaptor<CandidatoEmbedding> captor = ArgumentCaptor.forClass(CandidatoEmbedding.class);
        verify(candidatoEmbeddingRepository).save(captor.capture());
        assertThat(captor.getValue().getCandidato()).isSameAs(candidato);
        assertThat(captor.getValue().getVetor()).isEqualTo("1.0,0.0");
    }
}
