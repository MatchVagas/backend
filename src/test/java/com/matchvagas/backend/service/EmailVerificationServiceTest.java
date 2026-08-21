package com.matchvagas.backend.service;

import com.matchvagas.backend.entity.EmailVerificationToken;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.repository.EmailVerificationTokenRepository;
import com.matchvagas.backend.repository.UsuariosRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fase 1 — Verificação de e-mail")
class EmailVerificationServiceTest {

    @Mock UsuariosRepository usuariosRepository;
    @Mock EmailVerificationTokenRepository tokenRepository;
    @Mock EmailService emailService;

    @InjectMocks EmailVerificationService service;

    private Usuarios usuario(Boolean verificado) {
        Usuarios u = new Usuarios();
        u.setId(1L);
        u.setNome("João");
        u.setEmail("joao@email.com");
        u.setEmailVerificado(verificado);
        return u;
    }

    private EmailVerificationToken token(Usuarios u, boolean used, LocalDateTime expiresAt) {
        EmailVerificationToken t = new EmailVerificationToken();
        t.setToken("tok-123");
        t.setUsuario(u);
        t.setUsed(used);
        t.setExpiresAt(expiresAt);
        return t;
    }

    @Nested
    @DisplayName("enviarVerificacao")
    class Enviar {

        @Test
        @DisplayName("invalida tokens anteriores, salva novo e envia e-mail")
        void enviaEmail() {
            Usuarios u = usuario(false);

            service.enviarVerificacao(u);

            verify(tokenRepository).deleteByUsuarioId(1L);
            verify(tokenRepository).save(any(EmailVerificationToken.class));
            verify(emailService).enviarEmail(eq("joao@email.com"), any(), any());
        }
    }

    @Nested
    @DisplayName("confirmar")
    class Confirmar {

        @Test
        @DisplayName("token válido marca e-mail como verificado e consome o token")
        void confirmaComSucesso() {
            Usuarios u = usuario(false);
            EmailVerificationToken t = token(u, false, LocalDateTime.now().plusHours(1));
            when(tokenRepository.findByToken("tok-123")).thenReturn(Optional.of(t));

            service.confirmar("tok-123");

            assertThat(u.getEmailVerificado()).isTrue();
            assertThat(t.isUsed()).isTrue();
            verify(usuariosRepository).save(u);
            verify(tokenRepository).save(t);
        }

        @Test
        @DisplayName("token inexistente lança exceção")
        void tokenInexistente() {
            when(tokenRepository.findByToken("x")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.confirmar("x"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("inválido");
        }

        @Test
        @DisplayName("token expirado lança exceção")
        void tokenExpirado() {
            Usuarios u = usuario(false);
            EmailVerificationToken t = token(u, false, LocalDateTime.now().minusMinutes(1));
            when(tokenRepository.findByToken("tok-123")).thenReturn(Optional.of(t));

            assertThatThrownBy(() -> service.confirmar("tok-123"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("expirado");
            verify(usuariosRepository, never()).save(any());
        }

        @Test
        @DisplayName("token já usado, mas conta já verificada, é idempotente")
        void usadoEJaVerificado() {
            Usuarios u = usuario(true);
            EmailVerificationToken t = token(u, true, LocalDateTime.now().plusHours(1));
            when(tokenRepository.findByToken("tok-123")).thenReturn(Optional.of(t));

            service.confirmar("tok-123");

            verify(usuariosRepository, never()).save(any());
        }

        @Test
        @DisplayName("token já usado com conta ainda não verificada lança exceção")
        void usadoENaoVerificado() {
            Usuarios u = usuario(false);
            EmailVerificationToken t = token(u, true, LocalDateTime.now().plusHours(1));
            when(tokenRepository.findByToken("tok-123")).thenReturn(Optional.of(t));

            assertThatThrownBy(() -> service.confirmar("tok-123"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("já foi utilizado");
        }
    }

    @Nested
    @DisplayName("reenviar")
    class Reenviar {

        @Test
        @DisplayName("usuário não verificado recebe novo e-mail")
        void reenviaParaNaoVerificado() {
            when(usuariosRepository.findByEmail("joao@email.com"))
                    .thenReturn(Optional.of(usuario(false)));

            service.reenviar("joao@email.com");

            verify(emailService).enviarEmail(eq("joao@email.com"), any(), any());
        }

        @Test
        @DisplayName("usuário já verificado não recebe e-mail")
        void naoReenviaParaVerificado() {
            when(usuariosRepository.findByEmail("joao@email.com"))
                    .thenReturn(Optional.of(usuario(true)));

            service.reenviar("joao@email.com");

            verify(emailService, never()).enviarEmail(any(), any(), any());
            verify(tokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("e-mail inexistente é silencioso (sem enumeração)")
        void emailInexistente() {
            when(usuariosRepository.findByEmail("x@x.com")).thenReturn(Optional.empty());

            service.reenviar("x@x.com");

            verify(emailService, never()).enviarEmail(any(), any(), any());
        }
    }
}
