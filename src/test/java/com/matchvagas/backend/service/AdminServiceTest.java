package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.AdministradorRequestDTO;
import com.matchvagas.backend.dto.AdministradorResponseDTO;
import com.matchvagas.backend.dto.CandidatoResponseDTO;
import com.matchvagas.backend.dto.EmpresaResponseDTO;
import com.matchvagas.backend.dto.UsuarioResponseDTO;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RF010 — Administração do Sistema")
class AdminServiceTest {

    @Mock UsuariosRepository usuariosRepository;
    @Mock CandidatoRepository candidatoRepository;
    @Mock EmpresaRepository empresaRepository;
    @Mock AdministradoresRepository administradoresRepository;
    @Mock DepartamentosRepository departamentosRepository;
    @Mock UsuarioMapper usuarioMapper;
    @Mock CandidatoMapper candidatoMapper;
    @Mock EmpresaMapper empresaMapper;
    @Mock AdministradoresMapper administradoresMapper;

    @InjectMocks AdminService adminService;

    private Usuarios usuario;
    private Administradores admin;
    private Departamentos departamento;

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
            UsuarioResponseDTO dto = new UsuarioResponseDTO(
                    1L, "Admin Master", "admin@matchvagas.com", 30,
                    true, TipoUsuario.CANDIDATO, LocalDateTime.now(), LocalDateTime.now(), null);

            when(usuariosRepository.findAll()).thenReturn(List.of(usuario));
            when(usuarioMapper.toResponseDTO(usuario)).thenReturn(dto);

            List<UsuarioResponseDTO> result = adminService.listarUsuarios();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).nome()).isEqualTo("Admin Master");
        }

        @Test
        @DisplayName("Deve ativar usuário inativo")
        void deveAtivarUsuario() {
            usuario.setAtivo(false);
            when(usuariosRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(usuariosRepository.save(any())).thenReturn(usuario);

            adminService.ativarDesativarUsuario(1L, true);

            assertThat(usuario.getAtivo()).isTrue();
            verify(usuariosRepository).save(usuario);
        }

        @Test
        @DisplayName("Deve desativar usuário ativo")
        void deveDesativarUsuario() {
            when(usuariosRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(usuariosRepository.save(any())).thenReturn(usuario);

            adminService.ativarDesativarUsuario(1L, false);

            assertThat(usuario.getAtivo()).isFalse();
            verify(usuariosRepository).save(usuario);
        }

        @Test
        @DisplayName("Deve lançar exceção ao ativar/desativar usuário inexistente")
        void deveLancarExcecaoUsuarioNaoEncontrado() {
            when(usuariosRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.ativarDesativarUsuario(99L, true))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Deve excluir usuário do sistema")
        void deveExcluirUsuario() {
            when(usuariosRepository.existsById(1L)).thenReturn(true);
            doNothing().when(usuariosRepository).deleteById(1L);

            adminService.excluirUsuario(1L);

            verify(usuariosRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Deve lançar exceção ao excluir usuário inexistente")
        void deveLancarExcecaoExcluirUsuarioInexistente() {
            when(usuariosRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> adminService.excluirUsuario(99L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(usuariosRepository, never()).deleteById(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gestão de Administradores
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Gestão de Administradores")
    class GestaoAdmins {

        @Test
        @DisplayName("Deve promover usuário a administrador")
        void devePromoverUsuarioAAdmin() {
            AdministradorRequestDTO request = new AdministradorRequestDTO(
                    1L, "SUPER", 1L, "TUDO");

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

            assertThat(result).isNotNull();
            assertThat(result.nivel()).isEqualTo("SUPER");
            // Verifica que o tipo do usuário foi promovido para ADMIN
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
        @DisplayName("Deve remover administrador e reverter tipo do usuário")
        void deveRemoverAdminEReverterTipo() {
            usuario.setTipoUsuario(TipoUsuario.ADMIN);

            when(administradoresRepository.findById(1L)).thenReturn(Optional.of(admin));
            when(usuariosRepository.save(any())).thenReturn(usuario);
            doNothing().when(administradoresRepository).deleteById(1L);

            adminService.removerAdmin(1L);

            // Tipo deve ter voltado para CANDIDATO
            assertThat(usuario.getTipoUsuario()).isEqualTo(TipoUsuario.CANDIDATO);
            verify(administradoresRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Deve lançar exceção ao remover administrador inexistente")
        void deveLancarExcecaoAdminNaoEncontrado() {
            when(administradoresRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.removerAdmin(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Deve listar todos os administradores")
        void deveListarAdmins() {
            AdministradorResponseDTO dto = new AdministradorResponseDTO(
                    1L, "Admin Master", "admin@matchvagas.com", "SUPER", "TI", "TUDO");

            when(administradoresRepository.findAll()).thenReturn(List.of(admin));
            when(administradoresMapper.toResponseDTO(admin)).thenReturn(dto);

            List<AdministradorResponseDTO> result = adminService.listarAdmins();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).nomeUsuario()).isEqualTo("Admin Master");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Visão global — candidatos e empresas
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Visão global de candidatos e empresas")
    class VisaoGlobal {

        @Test
        @DisplayName("Deve listar todos os candidatos")
        void deveListarCandidatos() {
            Candidatos candidato = new Candidatos();
            candidato.setId(1L);
            candidato.setUsuario(usuario);

            CandidatoResponseDTO dto = new CandidatoResponseDTO(
                    1L, "Admin Master", "admin@matchvagas.com",
                    null, null, null, null);

            when(candidatoRepository.findAll()).thenReturn(List.of(candidato));
            when(candidatoMapper.toResponseDTO(candidato)).thenReturn(dto);

            List<CandidatoResponseDTO> result = adminService.listarCandidatos();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Deve listar todas as empresas")
        void deveListarEmpresas() {
            Empresas empresa = new Empresas();
            empresa.setId(1L);
            empresa.setNomeFantasia("Tech Corp");

            EmpresaResponseDTO dto = new EmpresaResponseDTO(
                    1L, "12.345.678/0001-90", "Tech Corp Ltda",
                    "Tech Corp", null, null, null, null, null,
                    1L, "Gestor Tech Corp"); // ATUALIZADO — usuarioGestorId e nomeGestor

            when(empresaRepository.findAll()).thenReturn(List.of(empresa));
            when(empresaMapper.toResponseDTO(empresa)).thenReturn(dto);

            List<EmpresaResponseDTO> result = adminService.listarEmpresas();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).nomeFantasia()).isEqualTo("Tech Corp");
        }
    }
}
