package com.matchvagas.backend.service;

import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Vagas;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.VagaRepository;
import com.matchvagas.backend.service.embedding.IndexacaoEmbeddingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fase 3 — backfill de embeddings (admin)")
class EmbeddingAdminServiceTest {

    @Mock VagaRepository vagaRepository;
    @Mock CandidatoRepository candidatoRepository;
    @Mock IndexacaoEmbeddingService indexacao;
    @InjectMocks EmbeddingAdminService service;

    @Test
    @DisplayName("desligado devolve inativo sem consultar o banco")
    void desligado() {
        when(indexacao.isAtivo()).thenReturn(false);

        var resposta = service.backfill();

        assertThat(resposta).isEqualTo(new com.matchvagas.backend.dto.EmbeddingBackfillResponseDTO(false, 0, 0, 0));
        verifyNoInteractions(vagaRepository, candidatoRepository);
    }

    @Test
    @DisplayName("conta só os registros efetivamente atualizados")
    void contaAtualizados() {
        when(indexacao.isAtivo()).thenReturn(true);
        when(vagaRepository.findAll()).thenReturn(List.of(new Vagas(), new Vagas()));
        when(candidatoRepository.findAll()).thenReturn(List.of(new Candidatos()));
        when(indexacao.indexarVaga(any())).thenReturn(true, false);
        when(indexacao.indexarCandidato(any())).thenReturn(true);

        var resposta = service.backfill();

        assertThat(resposta).isEqualTo(new com.matchvagas.backend.dto.EmbeddingBackfillResponseDTO(true, 2, 1, 2));
    }
}
