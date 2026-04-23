package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.CandidaturaRequestDTO;
import com.matchvagas.backend.dto.CandidaturaResponseDTO;
import com.matchvagas.backend.entity.*;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.CandidaturaMapper;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.CandidaturaRepository;
import com.matchvagas.backend.repository.VagaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RF008 e RF009 — Candidatura e Acompanhamento")
class CandidaturaServiceTest {

    @Mock CandidaturaRepository candidaturasRepository;
    @Mock CandidatoRepository candidatosRepository;
    @Mock VagaRepository vagasRepository;
    @Mock CandidaturaMapper candidaturaMapper;

    @InjectMocks CandidaturaService candidaturaService;

    private Candidatos candidato;
    private Vagas vaga;
    private Candidatura candidatura;
    private CandidaturaRequestDTO request;
    private CandidaturaResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        Usuarios usuario = new Usuarios();
        usuario.setId(1L);
        usuario.setNome("Carlos Souza");

        candidato = new Candidatos();
        candidato.setId(10L);
        candidato.setUsuario(usuario);

        StatusVaga statusAtiva = new StatusVaga();
        statusAtiva.setId(1L);
        statusAtiva.setDescricao("ATIVA");

        vaga = new Vagas();
        vaga.setId(5L);
        vaga.setTitulo("Dev Java Pleno");
        vaga.setStatus(statusAtiva);

        StatusCandidatura statusCandidatura = new StatusCandidatura();
        statusCandidatura.setId(1);
        statusCandidatura.setStatus("EM_ANALISE");

        candidatura = new Candidatura();
        candidatura.setId(100L);
        candidatura.setCandidato(candidato);
        candidatura.setVaga(vaga);
        candidatura.setStatus(statusCandidatura);
        candidatura.setDataCandidatura(LocalDateTime.now());

        request = new CandidaturaRequestDTO(5L);

        responseDTO = new CandidaturaResponseDTO(
                100L, 10L, "Carlos Souza", 5L, "Dev Java Pleno",
                LocalDateTime.now(), "EM_ANALISE"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RF008 — Candidatura à Vaga
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RF008 — Candidatura à Vaga")
    class RF008 {

        @Test
        @DisplayName("Deve registrar candidatura com sucesso")
        void deveRegistrarCandidaturaComSucesso() {
            when(vagasRepository.findById(5L)).thenReturn(Optional.of(vaga));
            when(candidatosRepository.findById(10L)).thenReturn(Optional.of(candidato));
            when(candidaturasRepository.existsByCandidatoIdAndVagaId(10L, 5L)).thenReturn(false);
            when(candidaturasRepository.save(any())).thenReturn(candidatura);
            when(candidaturaMapper.toResponseDTO(candidatura)).thenReturn(responseDTO);

            CandidaturaResponseDTO result = candidaturaService.candidatar(10L, request);

            assertThat(result).isNotNull();
            assertThat(result.vagaId()).isEqualTo(5L);
            assertThat(result.candidatoId()).isEqualTo(10L);
            assertThat(result.status()).isEqualTo("EM_ANALISE");
            verify(candidaturasRepository).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção para candidatura duplicada — RNF010")
        void deveLancarExcecaoCandidaturaDuplicada() {
            when(vagasRepository.findById(5L)).thenReturn(Optional.of(vaga));
            when(candidatosRepository.findById(10L)).thenReturn(Optional.of(candidato));
            when(candidaturasRepository.existsByCandidatoIdAndVagaId(10L, 5L)).thenReturn(true);

            assertThatThrownBy(() -> candidaturaService.candidatar(10L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("já se candidatou");

            verify(candidaturasRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando vaga não existe")
        void deveLancarExcecaoVagaNaoEncontrada() {
            when(vagasRepository.findById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> candidaturaService.candidatar(10L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Vaga");
        }

        @Test
        @DisplayName("Deve lançar exceção quando vaga não está ativa")
        void deveLancarExcecaoVagaInativa() {
            StatusVaga statusInativo = new StatusVaga();
            statusInativo.setDescricao("ENCERRADA");
            vaga.setStatus(statusInativo);

            when(vagasRepository.findById(5L)).thenReturn(Optional.of(vaga));

            assertThatThrownBy(() -> candidaturaService.candidatar(10L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("disponível");
        }

        @Test
        @DisplayName("Deve lançar exceção quando candidato não existe")
        void deveLancarExcecaoCandidatoNaoEncontrado() {
            when(vagasRepository.findById(5L)).thenReturn(Optional.of(vaga));
            when(candidatosRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> candidaturaService.candidatar(99L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Candidato");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RF009 — Acompanhamento de Candidaturas
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RF009 — Acompanhamento de Candidaturas")
    class RF009 {

        @Test
        @DisplayName("Deve listar todas as candidaturas do candidato com status")
        void deveListarCandidaturasComStatus() {
            when(candidaturasRepository.findByCandidatoId(10L)).thenReturn(List.of(candidatura));
            when(candidaturaMapper.toResponseDTO(candidatura)).thenReturn(responseDTO);

            List<CandidaturaResponseDTO> result = candidaturaService.findByCandidato(10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).status()).isEqualTo("EM_ANALISE");
            assertThat(result.get(0).tituloVaga()).isEqualTo("Dev Java Pleno");
        }

        @Test
        @DisplayName("Deve retornar lista vazia para candidato sem candidaturas")
        void deveRetornarVazioSemCandidaturas() {
            when(candidaturasRepository.findByCandidatoId(10L)).thenReturn(List.of());

            List<CandidaturaResponseDTO> result = candidaturaService.findByCandidato(10L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Deve retornar detalhe de candidatura do próprio candidato")
        void deveRetornarDetalheDaCandidatura() {
            when(candidaturasRepository.findById(100L)).thenReturn(Optional.of(candidatura));
            when(candidaturaMapper.toResponseDTO(candidatura)).thenReturn(responseDTO);

            CandidaturaResponseDTO result = candidaturaService.findByIdAndCandidato(100L, 10L);

            assertThat(result.id()).isEqualTo(100L);
            assertThat(result.nomeCandidato()).isEqualTo("Carlos Souza");
        }

        @Test
        @DisplayName("Deve lançar exceção quando candidatura não existe")
        void deveLancarExcecaoCandidaturaNaoEncontrada() {
            when(candidaturasRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> candidaturaService.findByIdAndCandidato(999L, 10L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Deve lançar exceção quando candidatura pertence a outro candidato")
        void deveLancarExcecaoAcessoNegado() {
            when(candidaturasRepository.findById(100L)).thenReturn(Optional.of(candidatura));

            // candidato 10 tenta acessar candidatura do candidato 10, mas passando id 99 (outro)
            assertThatThrownBy(() -> candidaturaService.findByIdAndCandidato(100L, 99L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("permissão");
        }
    }
}
