package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.ExperienciaRequestDTO;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Experiencia;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.ExperienciaRepository;
import com.matchvagas.backend.service.embedding.IndexacaoEmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RF003 — Experiências profissionais do candidato")
class ExperienciaServiceTest {

    @Mock ExperienciaRepository experienciaRepository;
    @Mock CandidatoRepository candidatoRepository;
    @Mock IndexacaoEmbeddingService indexacaoEmbeddingService;
    @Spy AposCommitExecutor aposCommitExecutor = new AposCommitExecutor();
    @InjectMocks ExperienciaService service;

    private Candidatos candidato;
    private final ExperienciaRequestDTO dto =
            new ExperienciaRequestDTO("ACME", "Dev Java", "APIs", "2020-01", null);

    @BeforeEach
    void setUp() {
        candidato = new Candidatos();
        candidato.setId(10L);
    }

    private Experiencia experiencia(Long id, Candidatos dono) {
        Experiencia e = new Experiencia();
        e.setId(id);
        e.setCandidato(dono);
        e.setCargo("Antigo");
        return e;
    }

    @Test
    @DisplayName("lista as experiências do candidato autenticado")
    void lista() {
        when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));
        when(experienciaRepository.findByCandidatoId(10L)).thenReturn(List.of(experiencia(3L, candidato)));

        assertThat(service.listar(1L)).singleElement()
                .satisfies(r -> assertThat(r.candidatoId()).isEqualTo(10L));
    }

    @Test
    @DisplayName("adiciona vinculando ao candidato e reindexa o perfil")
    void adiciona() {
        when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));
        when(experienciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resposta = service.adicionar(1L, dto);

        assertThat(resposta.cargo()).isEqualTo("Dev Java");
        assertThat(resposta.empresa()).isEqualTo("ACME");
        assertThat(resposta.candidatoId()).isEqualTo(10L);
        verify(indexacaoEmbeddingService).indexarCandidato(candidato);
    }

    @Test
    @DisplayName("atualiza a própria experiência")
    void atualiza() {
        when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));
        when(experienciaRepository.findById(3L)).thenReturn(Optional.of(experiencia(3L, candidato)));
        when(experienciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.atualizar(1L, 3L, dto).cargo()).isEqualTo("Dev Java");
        verify(indexacaoEmbeddingService).indexarCandidato(candidato);
    }

    @Test
    @DisplayName("não permite alterar nem remover experiência de outro candidato")
    void bloqueiaOutroCandidato() {
        Candidatos outro = new Candidatos();
        outro.setId(99L);
        when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));
        when(experienciaRepository.findById(3L)).thenReturn(Optional.of(experiencia(3L, outro)));

        assertThatThrownBy(() -> service.atualizar(1L, 3L, dto)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.remover(1L, 3L)).isInstanceOf(ResourceNotFoundException.class);
        verify(experienciaRepository, never()).save(any());
        verify(experienciaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("remove a própria experiência e reindexa")
    void remove() {
        Experiencia propria = experiencia(3L, candidato);
        when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));
        when(experienciaRepository.findById(3L)).thenReturn(Optional.of(propria));

        service.remover(1L, 3L);

        verify(experienciaRepository).delete(propria);
        verify(indexacaoEmbeddingService).indexarCandidato(candidato);
    }

    @Test
    @DisplayName("experiência inexistente ou usuário sem perfil retornam 404")
    void naoEncontrado() {
        when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));
        when(experienciaRepository.findById(3L)).thenReturn(Optional.empty());
        when(candidatoRepository.findByUsuarioId(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remover(1L, 3L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.listar(2L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
