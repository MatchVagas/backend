package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.AuthResponse;
import com.matchvagas.backend.dto.LoginRequestDTO;
import com.matchvagas.backend.dto.UsuarioResponseDTO;
import com.matchvagas.backend.dto.UsuariosRequestDTO;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.entity.Usuarios.TipoUsuario;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.mapper.UsuarioMapper;
import com.matchvagas.backend.repository.UsuariosRepository;
import com.matchvagas.backend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RF001 e RF002 — Cadastro e Autenticação")
class AuthServiceTest {

    @Mock UsuariosRepository usuariosRepository;
    @Mock UsuarioMapper      usuariosMapper;
    @Mock PasswordEncoder    passwordEncoder;
    @Mock JwtTokenProvider   jwtTokenProvider;

    @InjectMocks AuthService authService;

    private Usuarios       usuarioBase;
    private UsuariosRequestDTO requestCandidato;

    @BeforeEach
    void setUp() {
        usuarioBase = new Usuarios();
        usuarioBase.setId(1L);
        usuarioBase.setNome("João Silva");
        usuarioBase.setEmail("joao@email.com");
        usuarioBase.setSenha("$2a$12$hashBcrypt");
        usuarioBase.setAtivo(true);
        usuarioBase.setTipoUsuario(TipoUsuario.CANDIDATO);

        requestCandidato = new UsuariosRequestDTO(
                "João Silva",
                "joao@email.com",
                "senha123",
                LocalDateTime.of(1995, 5, 10, 0, 0),
                TipoUsuario.CANDIDATO,
                null
        );
    }

    private UsuarioResponseDTO buildUsuarioResponseDTO(Long id, String nome, String email, TipoUsuario tipo) {
        return new UsuarioResponseDTO(id, nome, email, 29, true, tipo,
                LocalDateTime.now(), LocalDateTime.now(), null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RF001 — Cadastro de Usuários
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RF001 — Cadastro de Usuários")
    class RF001 {

        @Test
        @DisplayName("Deve cadastrar candidato com sucesso")
        void deveCadastrarCandidatoComSucesso() {
            when(usuariosRepository.existsByEmail("joao@email.com")).thenReturn(false);
            when(usuariosMapper.toEntity(any())).thenReturn(usuarioBase);
            when(passwordEncoder.encode("senha123")).thenReturn("$2a$12$hashBcrypt");
            when(usuariosRepository.save(any())).thenReturn(usuarioBase);
            when(usuariosMapper.toDTO(any())).thenReturn(
                    buildUsuarioResponseDTO(1L, "João Silva", "joao@email.com", TipoUsuario.CANDIDATO));

            UsuarioResponseDTO result = authService.register(requestCandidato);

            assertThat(result.email()).isEqualTo("joao@email.com");
            assertThat(result.tipoUsuario()).isEqualTo(TipoUsuario.CANDIDATO);
            verify(usuariosRepository).save(any());
        }

        @Test
        @DisplayName("Deve cadastrar empresa com sucesso")
        void deveCadastrarEmpresaComSucesso() {
            UsuariosRequestDTO reqEmpresa = new UsuariosRequestDTO(
                    "Tech Corp", "rh@techcorp.com", "senha123",
                    LocalDateTime.of(1990, 1, 1, 0, 0), TipoUsuario.EMPRESA, null);

            Usuarios usuarioEmpresa = new Usuarios();
            usuarioEmpresa.setId(2L);
            usuarioEmpresa.setTipoUsuario(TipoUsuario.EMPRESA);
            usuarioEmpresa.setEmail("rh@techcorp.com");
            usuarioEmpresa.setAtivo(true);

            when(usuariosRepository.existsByEmail("rh@techcorp.com")).thenReturn(false);
            when(usuariosMapper.toEntity(any())).thenReturn(usuarioEmpresa);
            when(usuariosRepository.save(any())).thenReturn(usuarioEmpresa);
            when(usuariosMapper.toDTO(any())).thenReturn(
                    buildUsuarioResponseDTO(2L, "Tech Corp", "rh@techcorp.com", TipoUsuario.EMPRESA));

            UsuarioResponseDTO result = authService.register(reqEmpresa);

            assertThat(result.tipoUsuario()).isEqualTo(TipoUsuario.EMPRESA);
        }

        @Test
        @DisplayName("Deve lançar exceção quando e-mail já está cadastrado")
        void deveLancarExcecaoEmailDuplicado() {
            when(usuariosRepository.existsByEmail("joao@email.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(requestCandidato))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("email");

            verify(usuariosRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve armazenar senha com hash bcrypt, nunca em texto puro")
        void deveCriptografarSenha() {
            when(usuariosRepository.existsByEmail(anyString())).thenReturn(false);
            when(usuariosMapper.toEntity(any())).thenReturn(usuarioBase);
            when(passwordEncoder.encode("senha123")).thenReturn("$2a$12$hashBcrypt");
            when(usuariosRepository.save(any())).thenReturn(usuarioBase);
            when(usuariosMapper.toDTO(any())).thenReturn(
                    buildUsuarioResponseDTO(1L, "João", "joao@email.com", TipoUsuario.CANDIDATO));

            authService.register(requestCandidato);

            verify(passwordEncoder).encode("senha123");
            verify(usuariosRepository).save(argThat(u -> u.getSenha().equals("$2a$12$hashBcrypt")));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RF002 — Autenticação
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RF002 — Autenticação")
    class RF002 {

        @Test
        @DisplayName("Deve autenticar e retornar JWT com sucesso")
        void deveAutenticarERetornarJwt() {
            LoginRequestDTO login = new LoginRequestDTO("joao@email.com", "senha123");

            when(usuariosRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(usuarioBase));
            when(passwordEncoder.matches("senha123", "$2a$12$hashBcrypt")).thenReturn(true);
            when(jwtTokenProvider.generateToken(usuarioBase)).thenReturn("jwt.token.aqui");

            AuthResponse response = authService.login(login);

            assertThat(response.token()).isEqualTo("jwt.token.aqui");
            assertThat(response.tipo()).isEqualTo("Bearer");
            assertThat(response.perfil()).isEqualTo("CANDIDATO");
            assertThat(response.email()).isEqualTo("joao@email.com");
        }

        @Test
        @DisplayName("Deve retornar 401 para e-mail não cadastrado (não revela se existe)")
        void deveLancarBadCredentialsParaEmailInexistente() {
            // Correção de segurança: não lança 404, lança BadCredentialsException → 401
            // Evita enumerar e-mails válidos do sistema
            LoginRequestDTO login = new LoginRequestDTO("naoexiste@email.com", "senha123");

            when(usuariosRepository.findByEmail("naoexiste@email.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(login))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("inválidos");
        }

        @Test
        @DisplayName("Deve retornar 401 para senha incorreta")
        void deveLancarBadCredentialsParaSenhaIncorreta() {
            // Correção de segurança: não lança BusinessException (400), lança BadCredentialsException → 401
            LoginRequestDTO login = new LoginRequestDTO("joao@email.com", "senhaErrada");

            when(usuariosRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(usuarioBase));
            when(passwordEncoder.matches("senhaErrada", "$2a$12$hashBcrypt")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(login))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("inválidos");
        }

        @Test
        @DisplayName("E-mail inválido e senha errada retornam a mesma mensagem genérica")
        void mensagemDeErroDeveSerGenerica() {
            // Garante que não se revela se o problema é o e-mail ou a senha
            LoginRequestDTO loginEmailErrado = new LoginRequestDTO("nao@existe.com", "qualquer");
            when(usuariosRepository.findByEmail("nao@existe.com")).thenReturn(Optional.empty());

            LoginRequestDTO loginSenhaErrada = new LoginRequestDTO("joao@email.com", "errada");
            when(usuariosRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(usuarioBase));
            when(passwordEncoder.matches("errada", "$2a$12$hashBcrypt")).thenReturn(false);

            String msgEmailErrado = null;
            String msgSenhaErrada = null;

            try { authService.login(loginEmailErrado); } catch (BadCredentialsException e) { msgEmailErrado = e.getMessage(); }
            try { authService.login(loginSenhaErrada); } catch (BadCredentialsException e) { msgSenhaErrada = e.getMessage(); }

            assertThat(msgEmailErrado).isEqualTo(msgSenhaErrada);
        }

        @Test
        @DisplayName("Deve retornar 401 para usuário inativo")
        void deveLancarBadCredentialsParaUsuarioInativo() {
            usuarioBase.setAtivo(false);
            LoginRequestDTO login = new LoginRequestDTO("joao@email.com", "senha123");

            when(usuariosRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(usuarioBase));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(login))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("inativo");
        }

        @Test
        @DisplayName("Token JWT deve refletir o perfil real do usuário")
        void tokenDeveRefletirPerfilReal() {
            usuarioBase.setTipoUsuario(TipoUsuario.EMPRESA);
            LoginRequestDTO login = new LoginRequestDTO("joao@email.com", "senha123");

            when(usuariosRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(usuarioBase));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtTokenProvider.generateToken(any())).thenReturn("jwt.token");

            AuthResponse response = authService.login(login);

            assertThat(response.perfil()).isEqualTo("EMPRESA");
        }

        @Test
        @DisplayName("Token JWT deve conter o ID do usuário")
        void tokenDeveConterIdDoUsuario() {
            LoginRequestDTO login = new LoginRequestDTO("joao@email.com", "senha123");

            when(usuariosRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(usuarioBase));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtTokenProvider.generateToken(any())).thenReturn("jwt.token");

            AuthResponse response = authService.login(login);

            assertThat(response.usuarioId()).isEqualTo(1L);
            assertThat(response.nome()).isEqualTo("João Silva");
        }
    }
}
