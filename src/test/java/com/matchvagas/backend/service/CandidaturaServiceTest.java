package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.AtualizarCompartilhamentoRequestDTO;
import com.matchvagas.backend.dto.CandidaturaEmpresaResponseDTO;
import com.matchvagas.backend.dto.CandidaturaRequestDTO;
import com.matchvagas.backend.dto.CandidaturaResponseDTO;
import com.matchvagas.backend.dto.HistoricoStatusResponseDTO;
import com.matchvagas.backend.entity.*;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.CandidaturaMapper;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.CandidaturaRepository;
import com.matchvagas.backend.repository.EmpresaRepository;
import com.matchvagas.backend.repository.ExperienciaRepository;
import com.matchvagas.backend.repository.FormacaoRepository;
import com.matchvagas.backend.repository.HistoricoStatusCandidaturaRepository;
import com.matchvagas.backend.repository.StatusCandidaturaRepository;
import com.matchvagas.backend.repository.VagaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RF008 e RF009 — Candidatura e Acompanhamento")
class CandidaturaServiceTest {

    @Mock CandidaturaRepository       candidaturasRepository;
    @Mock CandidatoRepository         candidatosRepository;
    @Mock VagaRepository              vagasRepository;
    @Mock EmpresaRepository           empresaRepository;
    @Mock ExperienciaRepository       experienciaRepository;
    @Mock FormacaoRepository          formacaoRepository;
    @Mock StatusCandidaturaRepository statusCandidaturaRepository;
    @Mock CandidaturaMapper           candidaturaMapper;
    @Mock NotificacaoService          notificacaoService;
    @Mock HistoricoStatusCandidaturaRepository historicoStatusRepository;

    @InjectMocks CandidaturaService candidaturaService;

    private Candidatos candidato;
    private Vagas      vaga;
    private Candidatura candidatura;
    private Empresas    empresa;

    private static final Long USUARIO_EMPRESA_ID  = 20L;
    private static final Long EMPRESA_ID          = 5L;
    private static final Long USUARIO_CANDIDATO_ID = 1L;   // ID do usuário (JWT subject)
    private static final Long CANDIDATO_ID        = 10L;   // ID da entidade Candidatos
    private static final Long VAGA_ID             = 5L;
    private static final Long CANDIDATURA_ID      = 100L;

    @BeforeEach
    void setUp() {
        Usuarios usuarioCandidato = new Usuarios();
        usuarioCandidato.setId(1L);
        usuarioCandidato.setNome("Carlos Souza");
        usuarioCandidato.setTelefones(new ArrayList<>());

        candidato = new Candidatos();
        candidato.setId(CANDIDATO_ID);
        candidato.setUsuario(usuarioCandidato);
        candidato.setObjetivoProfissional("Desenvolvedor Java Sênior");
        candidato.setDisponibilidade("Imediata");
        candidato.setPretensaoSalarial(new BigDecimal("8000.00"));

        StatusVaga statusAtiva = new StatusVaga();
        statusAtiva.setId(1L);
        statusAtiva.setDescricao("ATIVA");

        Usuarios usuarioEmpresa = new Usuarios();
        usuarioEmpresa.setId(USUARIO_EMPRESA_ID);

        empresa = new Empresas();
        empresa.setId(EMPRESA_ID);
        empresa.setNomeFantasia("Tech Corp");
        empresa.setUsuario(usuarioEmpresa);

        vaga = new Vagas();
        vaga.setId(VAGA_ID);
        vaga.setTitulo("Dev Java Pleno");
        vaga.setStatus(statusAtiva);
        vaga.setEmpresas(empresa);

        StatusCandidatura statusCandidatura = new StatusCandidatura();
        statusCandidatura.setId(1);
        statusCandidatura.setStatus("EM_ANALISE");

        candidatura = new Candidatura();
        candidatura.setId(CANDIDATURA_ID);
        candidatura.setCandidato(candidato);
        candidatura.setVaga(vaga);
        candidatura.setStatus(statusCandidatura);
        candidatura.setDataCandidatura(LocalDateTime.now());
        // defaults definidos na entidade: profissional=true, contato=false
    }

    private CandidaturaResponseDTO buildResponseDTO() {
        return new CandidaturaResponseDTO(
                CANDIDATURA_ID, CANDIDATO_ID, "Carlos Souza", VAGA_ID, "Dev Java Pleno",
                LocalDateTime.now(), "EM_ANALISE",
                true, true, true, true, true, true, false, false
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RF008 — Candidatura à Vaga
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RF008 — Candidatura à Vaga")
    class RF008 {

        @Test
        @DisplayName("Deve registrar candidatura com preferências padrão")
        void deveRegistrarCandidaturaComDefaults() {
            CandidaturaRequestDTO request = new CandidaturaRequestDTO(
                    VAGA_ID, null, null, null, null, null, null, null, null
            );
            CandidaturaResponseDTO responseDTO = buildResponseDTO();

            when(vagasRepository.findById(VAGA_ID)).thenReturn(Optional.of(vaga));
            when(candidatosRepository.findByUsuarioId(USUARIO_CANDIDATO_ID)).thenReturn(Optional.of(candidato));
            when(candidaturasRepository.existsByCandidatoIdAndVagaId(CANDIDATO_ID, VAGA_ID)).thenReturn(false);
            when(candidaturasRepository.save(any())).thenReturn(candidatura);
            when(candidaturaMapper.toResponseDTO(any())).thenReturn(responseDTO);

            CandidaturaResponseDTO result = candidaturaService.candidatar(USUARIO_CANDIDATO_ID, request);

            assertThat(result).isNotNull();
            assertThat(result.vagaId()).isEqualTo(VAGA_ID);
            assertThat(result.candidatoId()).isEqualTo(CANDIDATO_ID);
            assertThat(result.status()).isEqualTo("EM_ANALISE");
            // Preferências padrão: dados profissionais compartilhados, contato privado
            assertThat(result.compartilharObjetivoProfissional()).isTrue();
            assertThat(result.compartilharTelefone()).isFalse();
            assertThat(result.compartilharEndereco()).isFalse();
            verify(candidaturasRepository).save(any(Candidatura.class));
        }

        @Test
        @DisplayName("Deve registrar candidatura com preferências explícitas")
        void deveRegistrarCandidaturaComPreferenciasExplicitas() {
            CandidaturaRequestDTO request = new CandidaturaRequestDTO(
                    VAGA_ID,
                    true,  // objetivo
                    false, // disponibilidade
                    false, // pretensão
                    false, // currículo
                    false, // experiências
                    false, // formações
                    true,  // telefone
                    false  // endereço
            );
            CandidaturaResponseDTO responseDTO = new CandidaturaResponseDTO(
                    CANDIDATURA_ID, CANDIDATO_ID, "Carlos Souza", VAGA_ID, "Dev Java Pleno",
                    LocalDateTime.now(), "EM_ANALISE",
                    true, false, false, false, false, false, true, false
            );

            when(vagasRepository.findById(VAGA_ID)).thenReturn(Optional.of(vaga));
            when(candidatosRepository.findByUsuarioId(USUARIO_CANDIDATO_ID)).thenReturn(Optional.of(candidato));
            when(candidaturasRepository.existsByCandidatoIdAndVagaId(CANDIDATO_ID, VAGA_ID)).thenReturn(false);
            when(candidaturasRepository.save(any(Candidatura.class))).thenAnswer(inv -> {
                Candidatura c = inv.getArgument(0);
                // Verifica que as preferências foram aplicadas antes de salvar
                assertThat(c.isCompartilharDisponibilidade()).isFalse();
                assertThat(c.isCompartilharTelefone()).isTrue();
                return candidatura;
            });
            when(candidaturaMapper.toResponseDTO(any())).thenReturn(responseDTO);

            CandidaturaResponseDTO result = candidaturaService.candidatar(USUARIO_CANDIDATO_ID, request);

            assertThat(result.compartilharObjetivoProfissional()).isTrue();
            assertThat(result.compartilharDisponibilidade()).isFalse();
            assertThat(result.compartilharTelefone()).isTrue();
        }

        @Test
        @DisplayName("Deve lançar exceção para candidatura duplicada")
        void deveLancarExcecaoCandidaturaDuplicada() {
            CandidaturaRequestDTO request = new CandidaturaRequestDTO(
                    VAGA_ID, null, null, null, null, null, null, null, null
            );

            when(vagasRepository.findById(VAGA_ID)).thenReturn(Optional.of(vaga));
            when(candidatosRepository.findByUsuarioId(USUARIO_CANDIDATO_ID)).thenReturn(Optional.of(candidato));
            when(candidaturasRepository.existsByCandidatoIdAndVagaId(CANDIDATO_ID, VAGA_ID)).thenReturn(true);

            assertThatThrownBy(() -> candidaturaService.candidatar(USUARIO_CANDIDATO_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("já se candidatou");

            verify(candidaturasRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando vaga não existe")
        void deveLancarExcecaoVagaNaoEncontrada() {
            CandidaturaRequestDTO request = new CandidaturaRequestDTO(
                    VAGA_ID, null, null, null, null, null, null, null, null
            );
            when(vagasRepository.findById(VAGA_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> candidaturaService.candidatar(USUARIO_CANDIDATO_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Vaga");
        }

        @Test
        @DisplayName("Deve lançar exceção quando vaga não está ativa")
        void deveLancarExcecaoVagaInativa() {
            CandidaturaRequestDTO request = new CandidaturaRequestDTO(
                    VAGA_ID, null, null, null, null, null, null, null, null
            );
            StatusVaga statusInativo = new StatusVaga();
            statusInativo.setDescricao("ENCERRADA");
            vaga.setStatus(statusInativo);

            when(vagasRepository.findById(VAGA_ID)).thenReturn(Optional.of(vaga));

            assertThatThrownBy(() -> candidaturaService.candidatar(USUARIO_CANDIDATO_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("disponível");
        }

        @Test
        @DisplayName("Deve lançar exceção quando candidato não existe")
        void deveLancarExcecaoCandidatoNaoEncontrado() {
            CandidaturaRequestDTO request = new CandidaturaRequestDTO(
                    VAGA_ID, null, null, null, null, null, null, null, null
            );
            when(vagasRepository.findById(VAGA_ID)).thenReturn(Optional.of(vaga));
            when(candidatosRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> candidaturaService.candidatar(99L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Candidato");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Preferências de Compartilhamento
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Preferências de Compartilhamento")
    class PreferenciasCompartilhamento {

        @Test
        @DisplayName("Deve aplicar defaults ao criar candidatura sem preferências")
        void deveAplicarDefaultsNaCriacao() {
            Candidatura[] savedCandidatura = new Candidatura[1];

            CandidaturaRequestDTO request = new CandidaturaRequestDTO(
                    VAGA_ID, null, null, null, null, null, null, null, null
            );

            when(vagasRepository.findById(VAGA_ID)).thenReturn(Optional.of(vaga));
            when(candidatosRepository.findByUsuarioId(USUARIO_CANDIDATO_ID)).thenReturn(Optional.of(candidato));
            when(candidaturasRepository.existsByCandidatoIdAndVagaId(any(), any())).thenReturn(false);
            when(candidaturasRepository.save(any(Candidatura.class))).thenAnswer(inv -> {
                savedCandidatura[0] = inv.getArgument(0);
                return candidatura;
            });
            when(candidaturaMapper.toResponseDTO(any())).thenReturn(buildResponseDTO());

            candidaturaService.candidatar(USUARIO_CANDIDATO_ID, request);

            Candidatura salva = savedCandidatura[0];
            assertThat(salva.isCompartilharObjetivoProfissional()).isTrue();
            assertThat(salva.isCompartilharDisponibilidade()).isTrue();
            assertThat(salva.isCompartilharPretensaoSalarial()).isTrue();
            assertThat(salva.isCompartilharCurriculo()).isTrue();
            assertThat(salva.isCompartilharExperiencias()).isTrue();
            assertThat(salva.isCompartilharFormacoes()).isTrue();
            assertThat(salva.isCompartilharTelefone()).isFalse();
            assertThat(salva.isCompartilharEndereco()).isFalse();
        }

        @Test
        @DisplayName("Deve atualizar preferências de candidatura existente")
        void deveAtualizarPreferencias() {
            candidatura.setCompartilharTelefone(false);
            candidatura.setCompartilharPretensaoSalarial(true);

            AtualizarCompartilhamentoRequestDTO dto = new AtualizarCompartilhamentoRequestDTO(
                    true, true, false, true, true, true, true, false
            );
            CandidaturaResponseDTO responseAtualizado = new CandidaturaResponseDTO(
                    CANDIDATURA_ID, CANDIDATO_ID, "Carlos Souza", VAGA_ID, "Dev Java Pleno",
                    LocalDateTime.now(), "EM_ANALISE",
                    true, true, false, true, true, true, true, false
            );

            when(candidaturasRepository.findById(CANDIDATURA_ID)).thenReturn(Optional.of(candidatura));
            when(candidaturasRepository.save(any())).thenReturn(candidatura);
            when(candidaturaMapper.toResponseDTO(any())).thenReturn(responseAtualizado);

            CandidaturaResponseDTO result = candidaturaService.atualizarCompartilhamento(
                    CANDIDATURA_ID, USUARIO_CANDIDATO_ID, dto
            );

            assertThat(result.compartilharPretensaoSalarial()).isFalse();
            assertThat(result.compartilharTelefone()).isTrue();
            // Verifica que a entidade foi modificada
            assertThat(candidatura.isCompartilharTelefone()).isTrue();
            assertThat(candidatura.isCompartilharPretensaoSalarial()).isFalse();
            verify(candidaturasRepository).save(candidatura);
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar candidatura de outro candidato")
        void deveLancarExcecaoAoAtualizarCandidaturaDeOutroCanditato() {
            AtualizarCompartilhamentoRequestDTO dto = new AtualizarCompartilhamentoRequestDTO(
                    true, true, true, true, true, true, true, true
            );

            when(candidaturasRepository.findById(CANDIDATURA_ID)).thenReturn(Optional.of(candidatura));

            assertThatThrownBy(() ->
                    candidaturaService.atualizarCompartilhamento(CANDIDATURA_ID, 999L, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("permissão");

            verify(candidaturasRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar candidatura inexistente")
        void deveLancarExcecaoCandidaturaInexistente() {
            AtualizarCompartilhamentoRequestDTO dto = new AtualizarCompartilhamentoRequestDTO(
                    true, true, true, true, true, true, true, true
            );

            when(candidaturasRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    candidaturaService.atualizarCompartilhamento(999L, CANDIDATO_ID, dto))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RF009 — Acompanhamento (Candidato)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RF009 — Acompanhamento de Candidaturas (Candidato)")
    class RF009Candidato {

        @Test
        @DisplayName("Deve listar candidaturas do candidato com preferências visíveis")
        void deveListarCandidaturasComPreferencias() {
            CandidaturaResponseDTO responseDTO = buildResponseDTO();

            when(candidatosRepository.findByUsuarioId(USUARIO_CANDIDATO_ID)).thenReturn(Optional.of(candidato));
            when(candidaturasRepository.findByCandidatoId(CANDIDATO_ID)).thenReturn(List.of(candidatura));
            when(candidaturaMapper.toResponseDTO(candidatura)).thenReturn(responseDTO);

            List<CandidaturaResponseDTO> result = candidaturaService.findByCandidato(USUARIO_CANDIDATO_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).status()).isEqualTo("EM_ANALISE");
            // Candidato consegue ver suas próprias preferências
            assertThat(result.get(0).compartilharObjetivoProfissional()).isTrue();
            assertThat(result.get(0).compartilharTelefone()).isFalse();
        }

        @Test
        @DisplayName("Deve retornar lista vazia para candidato sem candidaturas")
        void deveRetornarVazioSemCandidaturas() {
            when(candidatosRepository.findByUsuarioId(USUARIO_CANDIDATO_ID)).thenReturn(Optional.of(candidato));
            when(candidaturasRepository.findByCandidatoId(CANDIDATO_ID)).thenReturn(List.of());

            assertThat(candidaturaService.findByCandidato(USUARIO_CANDIDATO_ID)).isEmpty();
        }

        @Test
        @DisplayName("Deve retornar detalhe de candidatura do próprio candidato")
        void deveRetornarDetalheDaCandidatura() {
            CandidaturaResponseDTO responseDTO = buildResponseDTO();

            when(candidaturasRepository.findById(CANDIDATURA_ID)).thenReturn(Optional.of(candidatura));
            when(candidaturaMapper.toResponseDTO(candidatura)).thenReturn(responseDTO);

            CandidaturaResponseDTO result = candidaturaService.findByIdAndCandidato(CANDIDATURA_ID, USUARIO_CANDIDATO_ID);

            assertThat(result.id()).isEqualTo(CANDIDATURA_ID);
            assertThat(result.nomeCandidato()).isEqualTo("Carlos Souza");
        }

        @Test
        @DisplayName("Deve lançar exceção quando candidatura não existe")
        void deveLancarExcecaoCandidaturaNaoEncontrada() {
            when(candidaturasRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> candidaturaService.findByIdAndCandidato(999L, USUARIO_CANDIDATO_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Deve lançar exceção quando candidatura pertence a outro candidato")
        void deveLancarExcecaoAcessoNegado() {
            when(candidaturasRepository.findById(CANDIDATURA_ID)).thenReturn(Optional.of(candidatura));

            assertThatThrownBy(() -> candidaturaService.findByIdAndCandidato(CANDIDATURA_ID, 999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("permissão");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RF009 — Visão da Empresa (dados filtrados por privacidade)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RF009 — Candidaturas recebidas pela empresa (com filtro de privacidade)")
    class RF009Empresa {

        @Test
        @DisplayName("Empresa deve ver apenas dados autorizados pelo candidato")
        void deveRetornarDadosFiltradosPorPrivacidade() {
            // Candidato compartilha objetivo e disponibilidade, mas não pretensão nem contato
            candidatura.setCompartilharObjetivoProfissional(true);
            candidatura.setCompartilharDisponibilidade(true);
            candidatura.setCompartilharPretensaoSalarial(false);
            candidatura.setCompartilharTelefone(false);
            candidatura.setCompartilharEndereco(false);

            when(empresaRepository.findByUsuarioId(USUARIO_EMPRESA_ID)).thenReturn(Optional.of(empresa));
            when(candidaturasRepository.findByVagaEmpresasId(EMPRESA_ID)).thenReturn(List.of(candidatura));

            List<CandidaturaEmpresaResponseDTO> result = candidaturaService.findByEmpresa(USUARIO_EMPRESA_ID);

            assertThat(result).hasSize(1);
            CandidaturaEmpresaResponseDTO dto = result.get(0);
            assertThat(dto.nomeCandidato()).isEqualTo("Carlos Souza");
            assertThat(dto.objetivoProfissional()).isEqualTo("Desenvolvedor Java Sênior");
            assertThat(dto.disponibilidade()).isEqualTo("Imediata");
            // Pretensão não foi compartilhada → deve ser null
            assertThat(dto.pretensaoSalarial()).isNull();
            // Dados de contato não compartilhados → null
            assertThat(dto.telefones()).isNull();
            assertThat(dto.endereco()).isNull();
        }

        @Test
        @DisplayName("Empresa vê pretensão salarial quando candidato autoriza")
        void deveExibirPretensaoQuandoAutorizado() {
            candidatura.setCompartilharPretensaoSalarial(true);

            when(empresaRepository.findByUsuarioId(USUARIO_EMPRESA_ID)).thenReturn(Optional.of(empresa));
            when(candidaturasRepository.findByVagaEmpresasId(EMPRESA_ID)).thenReturn(List.of(candidatura));

            List<CandidaturaEmpresaResponseDTO> result = candidaturaService.findByEmpresa(USUARIO_EMPRESA_ID);

            assertThat(result.get(0).pretensaoSalarial()).isEqualTo(new BigDecimal("8000.00"));
        }

        @Test
        @DisplayName("Empresa não vê nenhum dado profissional quando candidato oculta tudo")
        void naoDeveExibirDadosQuandoCandidatoOcultaTudo() {
            candidatura.setCompartilharObjetivoProfissional(false);
            candidatura.setCompartilharDisponibilidade(false);
            candidatura.setCompartilharPretensaoSalarial(false);
            candidatura.setCompartilharCurriculo(false);
            candidatura.setCompartilharExperiencias(false);
            candidatura.setCompartilharFormacoes(false);
            candidatura.setCompartilharTelefone(false);
            candidatura.setCompartilharEndereco(false);

            when(empresaRepository.findByUsuarioId(USUARIO_EMPRESA_ID)).thenReturn(Optional.of(empresa));
            when(candidaturasRepository.findByVagaEmpresasId(EMPRESA_ID)).thenReturn(List.of(candidatura));

            List<CandidaturaEmpresaResponseDTO> result = candidaturaService.findByEmpresa(USUARIO_EMPRESA_ID);

            CandidaturaEmpresaResponseDTO dto = result.get(0);
            // Somente identificação básica é sempre visível
            assertThat(dto.nomeCandidato()).isEqualTo("Carlos Souza");
            assertThat(dto.vagaId()).isEqualTo(VAGA_ID);
            // Tudo mais é null
            assertThat(dto.objetivoProfissional()).isNull();
            assertThat(dto.disponibilidade()).isNull();
            assertThat(dto.pretensaoSalarial()).isNull();
            assertThat(dto.curriculoNomeArquivo()).isNull();
            assertThat(dto.telefones()).isNull();
            assertThat(dto.endereco()).isNull();
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando empresa não tem candidaturas")
        void deveRetornarVazioSemCandidaturasRecebidas() {
            when(empresaRepository.findByUsuarioId(USUARIO_EMPRESA_ID)).thenReturn(Optional.of(empresa));
            when(candidaturasRepository.findByVagaEmpresasId(EMPRESA_ID)).thenReturn(List.of());

            assertThat(candidaturaService.findByEmpresa(USUARIO_EMPRESA_ID)).isEmpty();
        }

        @Test
        @DisplayName("Deve lançar exceção quando usuário não tem empresa vinculada")
        void deveLancarExcecaoSemEmpresaVinculada() {
            when(empresaRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> candidaturaService.findByEmpresa(99L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Nenhuma empresa vinculada");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Filtro por Vaga Específica
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Filtro de Candidatos por Vaga Específica")
    class FiltroPorVaga {

        @Test
        @DisplayName("Deve listar candidatos de uma vaga específica da empresa")
        void deveListarCandidatosDaVaga() {
            when(empresaRepository.findByUsuarioId(USUARIO_EMPRESA_ID)).thenReturn(Optional.of(empresa));
            when(vagasRepository.findById(VAGA_ID)).thenReturn(Optional.of(vaga));
            when(candidaturasRepository.findByVagaId(VAGA_ID)).thenReturn(List.of(candidatura));

            List<CandidaturaEmpresaResponseDTO> result =
                    candidaturaService.findByVagaAndEmpresa(VAGA_ID, USUARIO_EMPRESA_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).vagaId()).isEqualTo(VAGA_ID);
            assertThat(result.get(0).tituloVaga()).isEqualTo("Dev Java Pleno");
        }

        @Test
        @DisplayName("Deve lançar exceção quando vaga não pertence à empresa")
        void deveLancarExcecaoVagaDeOutraEmpresa() {
            Empresas outraEmpresa = new Empresas();
            outraEmpresa.setId(99L);
            vaga.setEmpresas(outraEmpresa);

            when(empresaRepository.findByUsuarioId(USUARIO_EMPRESA_ID)).thenReturn(Optional.of(empresa));
            when(vagasRepository.findById(VAGA_ID)).thenReturn(Optional.of(vaga));

            assertThatThrownBy(() ->
                    candidaturaService.findByVagaAndEmpresa(VAGA_ID, USUARIO_EMPRESA_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("não pertence");
        }

        @Test
        @DisplayName("Deve lançar exceção quando vaga não existe")
        void deveLancarExcecaoVagaInexistente() {
            when(empresaRepository.findByUsuarioId(USUARIO_EMPRESA_ID)).thenReturn(Optional.of(empresa));
            when(vagasRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    candidaturaService.findByVagaAndEmpresa(999L, USUARIO_EMPRESA_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Vaga");
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando vaga não tem candidatos")
        void deveRetornarVazioQuandoVagaSemCandidatos() {
            when(empresaRepository.findByUsuarioId(USUARIO_EMPRESA_ID)).thenReturn(Optional.of(empresa));
            when(vagasRepository.findById(VAGA_ID)).thenReturn(Optional.of(vaga));
            when(candidaturasRepository.findByVagaId(VAGA_ID)).thenReturn(List.of());

            List<CandidaturaEmpresaResponseDTO> result =
                    candidaturaService.findByVagaAndEmpresa(VAGA_ID, USUARIO_EMPRESA_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Filtro por vaga aplica preferências de privacidade de cada candidato")
        void deveFiltrarPrivacidadeNaBuscaPorVaga() {
            candidatura.setCompartilharPretensaoSalarial(false);
            candidatura.setCompartilharTelefone(false);

            when(empresaRepository.findByUsuarioId(USUARIO_EMPRESA_ID)).thenReturn(Optional.of(empresa));
            when(vagasRepository.findById(VAGA_ID)).thenReturn(Optional.of(vaga));
            when(candidaturasRepository.findByVagaId(VAGA_ID)).thenReturn(List.of(candidatura));

            List<CandidaturaEmpresaResponseDTO> result =
                    candidaturaService.findByVagaAndEmpresa(VAGA_ID, USUARIO_EMPRESA_ID);

            assertThat(result.get(0).pretensaoSalarial()).isNull();
            assertThat(result.get(0).telefones()).isNull();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Atualização de Status (empresa) — guard de no-op + histórico
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Atualização de status da candidatura (empresa)")
    class AtualizarStatus {

        private StatusCandidatura novoStatus() {
            StatusCandidatura s = new StatusCandidatura();
            s.setId(2);
            s.setStatus("APROVADO");
            return s;
        }

        @Test
        @DisplayName("Deve mudar o status, registrar histórico e notificar o candidato")
        void deveMudarStatusRegistrarHistoricoENotificar() {
            StatusCandidatura novo = novoStatus();

            when(empresaRepository.findByUsuarioId(USUARIO_EMPRESA_ID)).thenReturn(Optional.of(empresa));
            when(candidaturasRepository.findById(CANDIDATURA_ID)).thenReturn(Optional.of(candidatura));
            when(statusCandidaturaRepository.findById(2L)).thenReturn(Optional.of(novo));
            when(candidaturasRepository.save(any())).thenReturn(candidatura);

            candidaturaService.atualizarStatusEmpresa(CANDIDATURA_ID, 2L, USUARIO_EMPRESA_ID);

            // Status aplicado na entidade
            assertThat(candidatura.getStatus().getStatus()).isEqualTo("APROVADO");

            // Histórico registrado com origem, destino e autor corretos
            ArgumentCaptor<HistoricoStatusCandidatura> captor =
                    ArgumentCaptor.forClass(HistoricoStatusCandidatura.class);
            verify(historicoStatusRepository).save(captor.capture());
            HistoricoStatusCandidatura h = captor.getValue();
            assertThat(h.getStatusAnterior().getStatus()).isEqualTo("EM_ANALISE");
            assertThat(h.getStatusNovo().getStatus()).isEqualTo("APROVADO");
            assertThat(h.getUsuario().getId()).isEqualTo(USUARIO_EMPRESA_ID);
            assertThat(h.getCandidatura()).isSameAs(candidatura);

            // Candidato notificado
            verify(notificacaoService).notificarPorTipo(eq(USUARIO_CANDIDATO_ID), any(), any(), eq("Candidatura"));
        }

        @Test
        @DisplayName("No-op: status igual ao atual não persiste, não registra histórico nem notifica")
        void deveIgnorarQuandoStatusInalterado() {
            // Status atual da candidatura no setUp tem id=1
            StatusCandidatura mesmoStatus = new StatusCandidatura();
            mesmoStatus.setId(1);
            mesmoStatus.setStatus("EM_ANALISE");

            when(empresaRepository.findByUsuarioId(USUARIO_EMPRESA_ID)).thenReturn(Optional.of(empresa));
            when(candidaturasRepository.findById(CANDIDATURA_ID)).thenReturn(Optional.of(candidatura));
            when(statusCandidaturaRepository.findById(1L)).thenReturn(Optional.of(mesmoStatus));

            candidaturaService.atualizarStatusEmpresa(CANDIDATURA_ID, 1L, USUARIO_EMPRESA_ID);

            verify(candidaturasRepository, never()).save(any());
            verify(historicoStatusRepository, never()).save(any());
            verifyNoInteractions(notificacaoService);
        }

        @Test
        @DisplayName("Deve lançar exceção quando a candidatura não pertence à empresa")
        void deveLancarExcecaoCandidaturaDeOutraEmpresa() {
            Empresas outraEmpresa = new Empresas();
            outraEmpresa.setId(99L);
            vaga.setEmpresas(outraEmpresa);

            when(empresaRepository.findByUsuarioId(USUARIO_EMPRESA_ID)).thenReturn(Optional.of(empresa));
            when(candidaturasRepository.findById(CANDIDATURA_ID)).thenReturn(Optional.of(candidatura));

            assertThatThrownBy(() ->
                    candidaturaService.atualizarStatusEmpresa(CANDIDATURA_ID, 2L, USUARIO_EMPRESA_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("não pertence");

            verify(historicoStatusRepository, never()).save(any());
            verifyNoInteractions(notificacaoService);
        }

        @Test
        @DisplayName("Deve listar o histórico de status da candidatura")
        void deveListarHistorico() {
            StatusCandidatura aprovado = novoStatus();

            HistoricoStatusCandidatura h = new HistoricoStatusCandidatura();
            h.setId(1L);
            h.setCandidatura(candidatura);
            h.setStatusAnterior(candidatura.getStatus());
            h.setStatusNovo(aprovado);
            h.setUsuario(empresa.getUsuario());

            when(empresaRepository.findByUsuarioId(USUARIO_EMPRESA_ID)).thenReturn(Optional.of(empresa));
            when(candidaturasRepository.findById(CANDIDATURA_ID)).thenReturn(Optional.of(candidatura));
            when(historicoStatusRepository.findByCandidaturaIdOrderByDataHoraDesc(CANDIDATURA_ID))
                    .thenReturn(List.of(h));

            List<HistoricoStatusResponseDTO> result =
                    candidaturaService.listarHistoricoEmpresa(CANDIDATURA_ID, USUARIO_EMPRESA_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).statusAnterior()).isEqualTo("EM_ANALISE");
            assertThat(result.get(0).statusNovo()).isEqualTo("APROVADO");
            assertThat(result.get(0).usuarioId()).isEqualTo(USUARIO_EMPRESA_ID);
        }
    }
}
