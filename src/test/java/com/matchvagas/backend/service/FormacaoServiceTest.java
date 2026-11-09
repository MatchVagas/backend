package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.FormacaoRequestDTO;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Formacao;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.FormacaoRepository;
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
@DisplayName("RF003 — Formações acadêmicas do candidato")
class FormacaoServiceTest {

    @Mock FormacaoRepository formacaoRepository;
    @Mock CandidatoRepository candidatoRepository;
    @Mock IndexacaoEmbeddingService indexacaoEmbeddingService;
    @Spy AposCommitExecutor aposCommitExecutor = new AposCommitExecutor();
    @InjectMocks FormacaoService service;

    private Candidatos candidato;
    private final FormacaoRequestDTO dto =
            new FormacaoRequestDTO("UFX", "Computação", "Superior", "2018-02", "2022-12");

    @BeforeEach
    void setUp() {
        candidato = new Candidatos();
        candidato.setId(10L);
    }

    private Formacao formacao(Long id, Candidatos dono) {
        Formacao f = new Formacao();
        f.setId(id);
        f.setCandidato(dono);
        return f;
    }

    @Test
    @DisplayName("lista as formações do candidato autenticado")
    void lista() {
        when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));
        when(formacaoRepository.findByCandidatoId(10L)).thenReturn(List.of(formacao(3L, candidato)));

        assertThat(service.listar(1L)).hasSize(1);
    }

    @Test
    @DisplayName("adiciona com todos os campos e reindexa o perfil")
    void adiciona() {
        when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));
        when(formacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resposta = service.adicionar(1L, dto);

        assertThat(resposta.curso()).isEqualTo("Computação");
        assertThat(resposta.instituicao()).isEqualTo("UFX");
        assertThat(resposta.nivel()).isEqualTo("Superior");
        verify(indexacaoEmbeddingService).indexarCandidato(candidato);
    }

    @Test
    @DisplayName("atualiza e remove a própria formação")
    void atualizaERemove() {
        Formacao propria = formacao(3L, candidato);
        when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));
        when(formacaoRepository.findById(3L)).thenReturn(Optional.of(propria));
        when(formacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.atualizar(1L, 3L, dto).dataFim()).isEqualTo("2022-12");
        service.remover(1L, 3L);

        verify(formacaoRepository).delete(propria);
    }

    @Test
    @DisplayName("não permite alterar nem remover formação de outro candidato")
    void bloqueiaOutroCandidato() {
        Candidatos outro = new Candidatos();
        outro.setId(99L);
        when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));
        when(formacaoRepository.findById(3L)).thenReturn(Optional.of(formacao(3L, outro)));

        assertThatThrownBy(() -> service.atualizar(1L, 3L, dto)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.remover(1L, 3L)).isInstanceOf(ResourceNotFoundException.class);
        verify(formacaoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("usuário sem perfil de candidato retorna 404")
    void semPerfil() {
        when(candidatoRepository.findByUsuarioId(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.adicionar(2L, dto)).isInstanceOf(ResourceNotFoundException.class);
    }
}
