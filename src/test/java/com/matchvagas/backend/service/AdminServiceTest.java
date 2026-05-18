package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.*;
import com.matchvagas.backend.entity.*;
import com.matchvagas.backend.entity.Usuarios.TipoUsuario;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.*;
import com.matchvagas.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RF010 — Administração do Sistema")
class AdminServiceTest {

    @Mock UsuariosRepository         usuariosRepository;
    @Mock CandidatoRepository        candidatoRepository;
    @Mock EmpresaRepository          empresaRepository;
    @Mock VagaRepository             vagaRepository;
    @Mock CandidaturaRepository      candidaturaRepository;
    @Mock AdministradoresRepository  administradoresRepository;
    @Mock DepartamentosRepository    departamentosRepository;
    @Mock StatusVagaRepository       statusVagaRepository;
    @Mock StatusCandidaturaRepository statusCandidaturaRepository;
    @Mock UsuarioMapper              usuarioMapper;
    @Mock CandidatoMapper            candidatoMapper;
    @Mock EmpresaMapper              empresaMapper;
    @Mock VagasMapper                vagasMapper;
    @Mock CandidaturaMapper          candidaturaMapper;
    @Mock AdministradoresMapper      administradoresMapper;
    @Mock PasswordEncoder            passwordEncoder;

    @InjectMocks AdminService adminService;

    private Usuarios     usuario;
    private Administradores admin;
    private Departamentos   departamento;
    private Candidatos      candidato;
    private Empresas        empresa;
    private Vagas           vaga;
    private Candidatura     candidatura;
    private StatusVaga      statusVaga;
    private StatusCandidatura statusCandidatura;

    @BeforeEach
    void setUp() {
        usuario = new Usuarios();
        usuario.setId(1L);
        usuario.setNome("Admin Master");
        usuario.setEmail("admin@matchvagas.com");
        usuario.setAtivo(true);
        usuario.setTipoUsuario(TipoUsuario.CANDIDATO);

        departamento = new Departamentos();
        departamento.setId(1L);
        departamento.setNome("TI");

        admin = new Administradores();
        admin.setId(1L);
        admin.setUsuario(usuario);
        admin.setNivel("SUPER");
        admin.setDepartamento(departamento);
        admin.setPermissoes("TUDO");

        candidato = new Candidatos();
        candidato.setId(1L);
        candidato.setUsuario(usuario);

        empresa = new Empresas();
        empresa.setId(1L);
        empresa.setNomeFantasia("Tech Corp");

        statusVaga = new StatusVaga();
        statusVaga.setId(1L);
        statusVaga.setDescricao("ATIVA");

        vaga = new Vagas();
        vaga.setId(1L);
        vaga.setTitulo("Dev Java Pleno");
        vaga.setStatus(statusVaga);
        vaga.setEmpresas(empresa);

        statusCandidatura = new StatusCandidatura();
        statusCandidatura.setId(1);
        statusCandidatura.setStatus("EM_ANALISE");

        candidatura = new Candidatura();
        candidatura.setId(1L);
        candidatura.setCandidato(candidato);
        candidatura.setVaga(vaga);
        candidatura.setStatus(statusCandidatura);
    }

    private UsuarioResponseDTO buildUsuarioDTO() {
        return new UsuarioResponseDTO(1L, "Admin Master", "admin@matchvagas.com",
                30, true, TipoUsuario.CANDIDATO, LocalDateTime.now(), LocalDateTime.now(), null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gestão de Usuários
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Gestão de Usuários")
    class GestaoUsuarios {

        @Test
        @DisplayName("Deve listar todos os usuários do sistema")
        void deveListarTodosUsuarios() {
            when(usuariosRepository.findAll()).thenReturn(List.of(usuario));
            when(usuarioMapper.toResponseDTO(usuario)).thenReturn(buildUsuarioDTO());

            List<UsuarioResponseDTO> result = adminService.listarUsuarios();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).nome()).isEqualTo("Admin Master");
        }

        @Test
        @DisplayName("Deve buscar usuário por ID")
        void deveBuscarUsuarioPorId() {
            when(usuariosRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(usuarioMapper.toResponseDTO(usuario)).thenReturn(buildUsuarioDTO());

            UsuarioResponseDTO result = adminService.buscarUsuario(1L);

            assertThat(result.id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Deve lançar 404 para usuário inexistente")
        void deveLancarNotFoundParaUsuarioInexistente() {
            when(usuariosRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.buscarUsuario(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Deve ativar usuário inativo")
        void deveAtivarUsuario() {
            usuario.setAtivo(false);
            when(usuariosRepository.findById(1L)).thenReturn(Optional.of(usuario));

            adminService.ativarDesativarUsuario(1L, true);

            assertThat(usuario.getAtivo()).isTrue();
            verify(usuariosRepository).save(usuario);
        }

        @Test
        @DisplayName("Deve desativar usuário ativo")
        void deveDesativarUsuario() {
            when(usuariosRepository.findById(1L)).thenReturn(Optional.of(usuario));

            adminService.ativarDesativarUsuario(1L, false);

            assertThat(usuario.getAtivo()).isFalse();
            verify(usuariosRepository).save(usuario);
        }

        @Test
        @DisplayName("Deve alterar tipo do usuário")
        void deveAlterarTipoDoUsuario() {
            when(usuariosRepository.findById(1L)).thenReturn(Optional.of(usuario));

            adminService.alterarTipoUsuario(1L, TipoUsuario.EMPRESA);

            assertThat(usuario.getTipoUsuario()).isEqualTo(TipoUsuario.EMPRESA);
            verify(usuariosRepository).save(usuario);
        }

        @Test
        @DisplayName("Deve excluir usuário do sistema")
        void deveExcluirUsuario() {
            when(usuariosRepository.existsById(1L)).thenReturn(true);

            adminService.excluirUsuario(1L);

            verify(usuariosRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Deve lançar exceção ao excluir usuário inexistente")
        void deveLancarExcecaoExcluirInexistente() {
            when(usuariosRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> adminService.excluirUsuario(99L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(usuariosRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Deve atualizar dados do usuário")
        void deveAtualizarUsuario() {
            AdminAtualizarUsuarioRequestDTO dto = new AdminAtualizarUsuarioRequestDTO(
                    "Novo Nome", "novo@email.com", TipoUsuario.EMPRESA,
                    LocalDateTime.of(1990, 1, 1, 0, 0), true, null
            );

            when(usuariosRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(usuariosRepository.existsByEmail("novo@email.com")).thenReturn(false);
            when(usuariosRepository.save(any())).thenReturn(usuario);
            when(usuarioMapper.toResponseDTO(any())).thenReturn(buildUsuarioDTO());

            adminService.atualizarUsuario(1L, dto);

            assertThat(usuario.getNome()).isEqualTo("Novo Nome");
            assertThat(usuario.getEmail()).isEqualTo("novo@email.com");
            assertThat(usuario.getTipoUsuario()).isEqualTo(TipoUsuario.EMPRESA);
            verify(usuariosRepository).save(usuario);
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar com e-mail já usado por outro usuário")
        void deveLancarExcecaoEmailDuplicado() {
            AdminAtualizarUsuarioRequestDTO dto = new AdminAtualizarUsuarioRequestDTO(
                    "Novo Nome", "outro@email.com", TipoUsuario.CANDIDATO,
                    LocalDateTime.of(1990, 1, 1, 0, 0), true, null
            );

            when(usuariosRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(usuariosRepository.existsByEmail("outro@email.com")).thenReturn(true);

            assertThatThrownBy(() -> adminService.atualizarUsuario(1L, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("e-mail");
        }

        @Test
        @DisplayName("Deve atualizar senha quando fornecida")
        void deveAtualizarSenhaQuandoFornecida() {
            // E-mail igual ao atual: a verificação de duplicidade não é acionada
            AdminAtualizarUsuarioRequestDTO dto = new AdminAtualizarUsuarioRequestDTO(
                    "Admin Master", "admin@matchvagas.com", TipoUsuario.CANDIDATO,
                    LocalDateTime.of(1990, 1, 1, 0, 0), true, "novaSenha123"
            );

            when(usuariosRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(passwordEncoder.encode("novaSenha123")).thenReturn("$hash$nova");
            when(usuariosRepository.save(any())).thenReturn(usuario);
            when(usuarioMapper.toResponseDTO(any())).thenReturn(buildUsuarioDTO());

            adminService.atualizarUsuario(1L, dto);

            verify(passwordEncoder).encode("novaSenha123");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gestão de Candidatos
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Gestão de Candidatos")
    class GestaoCandidatos {

        @Test
        @DisplayName("Deve listar todos os candidatos")
        void deveListarCandidatos() {
            CandidatoResponseDTO dto = new CandidatoResponseDTO(
                    1L, "Admin Master", "admin@matchvagas.com", null, null, null, null, null, null, null, null);

            when(candidatoRepository.findAll()).thenReturn(List.of(candidato));
            when(candidatoMapper.toResponseDTO(candidato)).thenReturn(dto);

            assertThat(adminService.listarCandidatos()).hasSize(1);
        }

        @Test
        @DisplayName("Deve buscar candidato por ID")
        void deveBuscarCandidatoPorId() {
            CandidatoResponseDTO dto = new CandidatoResponseDTO(
                    1L, "Admin Master", "admin@matchvagas.com", null, null, null, null, null, null, null, null);

            when(candidatoRepository.findById(1L)).thenReturn(Optional.of(candidato));
            when(candidatoMapper.toResponseDTO(candidato)).thenReturn(dto);

            assertThat(adminService.buscarCandidato(1L).id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Deve lançar exceção para candidato inexistente")
        void deveLancarNotFoundParaCandidatoInexistente() {
            when(candidatoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.buscarCandidato(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Deve excluir candidato")
        void deveExcluirCandidato() {
            when(candidatoRepository.existsById(1L)).thenReturn(true);

            adminService.excluirCandidato(1L);

            verify(candidatoRepository).deleteById(1L);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gestão de Empresas
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Gestão de Empresas")
    class GestaoEmpresas {

        @Test
        @DisplayName("Deve listar todas as empresas")
        void deveListarEmpresas() {
            EmpresaResponseDTO dto = new EmpresaResponseDTO(
                    1L, "12.345.678/0001-90", "Tech Corp Ltda", "Tech Corp",
                    null, null, null, null, null, null, 1L, "Gestor", "PENDENTE");

            when(empresaRepository.findAll()).thenReturn(List.of(empresa));
            when(empresaMapper.toResponseDTO(empresa)).thenReturn(dto);

            assertThat(adminService.listarEmpresas()).hasSize(1);
        }

        @Test
        @DisplayName("Deve buscar empresa por ID")
        void deveBuscarEmpresaPorId() {
            EmpresaResponseDTO dto = new EmpresaResponseDTO(
                    1L, "12.345.678/0001-90", "Tech Corp Ltda", "Tech Corp",
                    null, null, null, null, null, null, 1L, "Gestor", "PENDENTE");

            when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
            when(empresaMapper.toResponseDTO(empresa)).thenReturn(dto);

            assertThat(adminService.buscarEmpresa(1L).id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Deve excluir empresa")
        void deveExcluirEmpresa() {
            when(empresaRepository.existsById(1L)).thenReturn(true);

            adminService.excluirEmpresa(1L);

            verify(empresaRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Deve lançar exceção ao excluir empresa inexistente")
        void deveLancarExcecaoEmpresaInexistente() {
            when(empresaRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> adminService.excluirEmpresa(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gestão de Vagas
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Gestão de Vagas")
    class GestaoVagas {

        @Test
        @DisplayName("Deve listar todas as vagas do sistema")
        void deveListarVagas() {
            when(vagaRepository.findAll()).thenReturn(List.of(vaga));
            when(vagasMapper.toDTO(vaga)).thenReturn(mock(VagaResponseDTO.class));

            assertThat(adminService.listarVagas()).hasSize(1);
        }

        @Test
        @DisplayName("Deve buscar vaga por ID")
        void deveBuscarVagaPorId() {
            VagaResponseDTO dto = mock(VagaResponseDTO.class);
            when(vagaRepository.findById(1L)).thenReturn(Optional.of(vaga));
            when(vagasMapper.toDTO(vaga)).thenReturn(dto);

            assertThat(adminService.buscarVaga(1L)).isEqualTo(dto);
        }

        @Test
        @DisplayName("Deve alterar status da vaga")
        void deveAlterarStatusDaVaga() {
            StatusVaga novoStatus = new StatusVaga();
            novoStatus.setId(2L);
            novoStatus.setDescricao("ENCERRADA");

            when(vagaRepository.findById(1L)).thenReturn(Optional.of(vaga));
            when(statusVagaRepository.findById(2L)).thenReturn(Optional.of(novoStatus));
            when(vagaRepository.save(any())).thenReturn(vaga);
            when(vagasMapper.toDTO(any())).thenReturn(mock(VagaResponseDTO.class));

            adminService.alterarStatusVaga(1L, 2L);

            assertThat(vaga.getStatus().getDescricao()).isEqualTo("ENCERRADA");
            verify(vagaRepository).save(vaga);
        }

        @Test
        @DisplayName("Deve lançar exceção para status de vaga inexistente")
        void deveLancarExcecaoStatusVagaInexistente() {
            when(vagaRepository.findById(1L)).thenReturn(Optional.of(vaga));
            when(statusVagaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.alterarStatusVaga(1L, 99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Deve excluir vaga do sistema")
        void deveExcluirVaga() {
            when(vagaRepository.existsById(1L)).thenReturn(true);

            adminService.excluirVaga(1L);

            verify(vagaRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Deve lançar exceção ao excluir vaga inexistente")
        void deveLancarExcecaoVagaInexistente() {
            when(vagaRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> adminService.excluirVaga(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gestão de Candidaturas
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Gestão de Candidaturas")
    class GestaoCandidaturas {

        @Test
        @DisplayName("Deve listar todas as candidaturas do sistema")
        void deveListarCandidaturas() {
            CandidaturaResponseDTO dto = new CandidaturaResponseDTO(
                    1L, 1L, "Carlos", 1L, "Dev Java", LocalDateTime.now(), "EM_ANALISE",
                    true, true, true, true, true, true, false, false
            );

            when(candidaturaRepository.findAll()).thenReturn(List.of(candidatura));
            when(candidaturaMapper.toResponseDTO(candidatura)).thenReturn(dto);

            assertThat(adminService.listarCandidaturas()).hasSize(1);
        }

        @Test
        @DisplayName("Deve buscar candidatura por ID")
        void deveBuscarCandidaturaPorId() {
            CandidaturaResponseDTO dto = new CandidaturaResponseDTO(
                    1L, 1L, "Carlos", 1L, "Dev Java", LocalDateTime.now(), "EM_ANALISE",
                    true, true, true, true, true, true, false, false
            );

            when(candidaturaRepository.findById(1L)).thenReturn(Optional.of(candidatura));
            when(candidaturaMapper.toResponseDTO(candidatura)).thenReturn(dto);

            assertThat(adminService.buscarCandidatura(1L).id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Deve alterar status da candidatura")
        void deveAlterarStatusDaCandidatura() {
            StatusCandidatura novoStatus = new StatusCandidatura();
            novoStatus.setId(2);
            novoStatus.setStatus("APROVADO");

            CandidaturaResponseDTO dto = new CandidaturaResponseDTO(
                    1L, 1L, "Carlos", 1L, "Dev Java", LocalDateTime.now(), "APROVADO",
                    true, true, true, true, true, true, false, false
            );

            when(candidaturaRepository.findById(1L)).thenReturn(Optional.of(candidatura));
            when(statusCandidaturaRepository.findById(2L)).thenReturn(Optional.of(novoStatus));
            when(candidaturaRepository.save(any())).thenReturn(candidatura);
            when(candidaturaMapper.toResponseDTO(any())).thenReturn(dto);

            CandidaturaResponseDTO result = adminService.alterarStatusCandidatura(1L, 2L);

            assertThat(candidatura.getStatus().getStatus()).isEqualTo("APROVADO");
            assertThat(result.status()).isEqualTo("APROVADO");
        }

        @Test
        @DisplayName("Deve lançar exceção para status de candidatura inexistente")
        void deveLancarExcecaoStatusCandidaturaInexistente() {
            when(candidaturaRepository.findById(1L)).thenReturn(Optional.of(candidatura));
            when(statusCandidaturaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.alterarStatusCandidatura(1L, 99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Deve excluir candidatura")
        void deveExcluirCandidatura() {
            when(candidaturaRepository.existsById(1L)).thenReturn(true);

            adminService.excluirCandidatura(1L);

            verify(candidaturaRepository).deleteById(1L);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gestão de Administradores
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Gestão de Administradores")
    class GestaoAdmins {

        @Test
        @DisplayName("Deve promover usuário a administrador e atualizar seu tipo")
        void devePromoverUsuarioAAdmin() {
            AdministradorRequestDTO request = new AdministradorRequestDTO(1L, "SUPER", 1L, "TUDO");
            AdministradorResponseDTO responseDTO = new AdministradorResponseDTO(
                    1L, "Admin Master", "admin@matchvagas.com", "SUPER", "TI", "TUDO");

            when(usuariosRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(administradoresRepository.findByUsuarioId(1L)).thenReturn(List.of());
            when(administradoresMapper.toEntity(request)).thenReturn(admin);
            when(departamentosRepository.findById(1L)).thenReturn(Optional.of(departamento));
            when(administradoresRepository.save(any())).thenReturn(admin);
            when(administradoresMapper.toResponseDTO(admin)).thenReturn(responseDTO);
            when(usuariosRepository.save(any())).thenReturn(usuario);

            AdministradorResponseDTO result = adminService.criarAdmin(request);

            assertThat(result.nivel()).isEqualTo("SUPER");
            assertThat(usuario.getTipoUsuario()).isEqualTo(TipoUsuario.ADMIN);
        }

        @Test
        @DisplayName("Deve lançar exceção quando usuário já é administrador")
        void deveLancarExcecaoUsuarioJaAdmin() {
            AdministradorRequestDTO request = new AdministradorRequestDTO(1L, "SUPER", 1L, "TUDO");

            when(usuariosRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(administradoresRepository.findByUsuarioId(1L)).thenReturn(List.of(admin));

            assertThatThrownBy(() -> adminService.criarAdmin(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("já é administrador");
        }

        @Test
        @DisplayName("Deve remover administrador e reverter tipo do usuário para CANDIDATO")
        void deveRemoverAdminEReverterTipo() {
            usuario.setTipoUsuario(TipoUsuario.ADMIN);

            when(administradoresRepository.findById(1L)).thenReturn(Optional.of(admin));
            when(usuariosRepository.save(any())).thenReturn(usuario);

            adminService.removerAdmin(1L);

            assertThat(usuario.getTipoUsuario()).isEqualTo(TipoUsuario.CANDIDATO);
            verify(administradoresRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Deve atualizar dados do administrador")
        void deveAtualizarAdmin() {
            AdministradorRequestDTO request = new AdministradorRequestDTO(1L, "GERENTE", 1L, "PARCIAL");
            AdministradorResponseDTO responseDTO = new AdministradorResponseDTO(
                    1L, "Admin Master", "admin@matchvagas.com", "GERENTE", "TI", "PARCIAL");

            when(administradoresRepository.findById(1L)).thenReturn(Optional.of(admin));
            when(departamentosRepository.findById(1L)).thenReturn(Optional.of(departamento));
            when(administradoresRepository.save(any())).thenReturn(admin);
            when(administradoresMapper.toResponseDTO(any())).thenReturn(responseDTO);

            AdministradorResponseDTO result = adminService.atualizarAdmin(1L, request);

            assertThat(admin.getNivel()).isEqualTo("GERENTE");
            assertThat(result.nivel()).isEqualTo("GERENTE");
        }

        @Test
        @DisplayName("Deve buscar administrador por ID")
        void deveBuscarAdminPorId() {
            AdministradorResponseDTO dto = new AdministradorResponseDTO(
                    1L, "Admin Master", "admin@matchvagas.com", "SUPER", "TI", "TUDO");

            when(administradoresRepository.findById(1L)).thenReturn(Optional.of(admin));
            when(administradoresMapper.toResponseDTO(admin)).thenReturn(dto);

            assertThat(adminService.buscarAdmin(1L).id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Deve lançar exceção ao remover admin inexistente")
        void deveLancarExcecaoAdminNaoEncontrado() {
            when(administradoresRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.removerAdmin(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Estatísticas do Sistema
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Estatísticas do Sistema")
    class Estatisticas {

        @Test
        @DisplayName("Deve retornar contagens corretas de todos os recursos")
        void deveRetornarEstatisticasCompletas() {
            when(usuariosRepository.count()).thenReturn(10L);
            when(usuariosRepository.findByAtivo(true)).thenReturn(List.of(usuario, usuario, usuario));
            when(candidatoRepository.count()).thenReturn(5L);
            when(empresaRepository.count()).thenReturn(3L);
            when(vagaRepository.count()).thenReturn(20L);
            when(candidaturaRepository.count()).thenReturn(50L);
            when(administradoresRepository.count()).thenReturn(2L);

            EstatisticasSistemaDTO result = adminService.estatisticas();

            assertThat(result.totalUsuarios()).isEqualTo(10L);
            assertThat(result.usuariosAtivos()).isEqualTo(3L);
            assertThat(result.usuariosInativos()).isEqualTo(7L);
            assertThat(result.totalCandidatos()).isEqualTo(5L);
            assertThat(result.totalEmpresas()).isEqualTo(3L);
            assertThat(result.totalVagas()).isEqualTo(20L);
            assertThat(result.totalCandidaturas()).isEqualTo(50L);
            assertThat(result.totalAdmins()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Inativos deve ser totalUsuarios menos usuariosAtivos")
        void inativosDeveSerComplementoDeAtivos() {
            when(usuariosRepository.count()).thenReturn(100L);
            when(usuariosRepository.findByAtivo(true)).thenReturn(
                    List.of(new Usuarios(), new Usuarios(), new Usuarios()));
            when(candidatoRepository.count()).thenReturn(0L);
            when(empresaRepository.count()).thenReturn(0L);
            when(vagaRepository.count()).thenReturn(0L);
            when(candidaturaRepository.count()).thenReturn(0L);
            when(administradoresRepository.count()).thenReturn(0L);

            EstatisticasSistemaDTO result = adminService.estatisticas();

            assertThat(result.usuariosAtivos() + result.usuariosInativos())
                    .isEqualTo(result.totalUsuarios());
        }
    }
}
