package com.matchvagas.backend.service;

import com.matchvagas.backend.entity.PasswordResetToken;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.repository.PasswordResetTokenRepository;
import com.matchvagas.backend.repository.UsuariosRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Redefinição de senha por código")
class PasswordResetServiceTest {

    @Mock UsuariosRepository usuariosRepository;
    @Mock PasswordResetTokenRepository tokenRepository;
    @Mock EmailService emailService;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks PasswordResetService service;

    private Usuarios usuario() {
        Usuarios u = new Usuarios();
        u.setId(1L);
        u.setNome("Ana");
        u.setEmail("ana@x.com");
        return u;
    }

    private PasswordResetToken token(String codigo) {
        PasswordResetToken t = new PasswordResetToken();
        t.setToken("tok-1");
        t.setCodigo(codigo);
        t.setUsuario(usuario());
        t.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        return t;
    }

    @Test
    @DisplayName("solicitação gera código de 6 dígitos, invalida os anteriores e envia por e-mail")
    void solicita() {
        when(usuariosRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(usuario()));

        service.solicitarRedefinicao("ana@x.com");

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).deleteByUsuarioId(1L);
        verify(tokenRepository).save(captor.capture());
        String codigo = captor.getValue().getCodigo();
        assertThat(codigo).matches("\\d{6}");
        verify(emailService).enviarEmail(eq("ana@x.com"), anyString(), contains(codigo));
    }

    @Test
    @DisplayName("e-mail desconhecido não revela nada nem envia e-mail")
    void emailDesconhecido() {
        when(usuariosRepository.findByEmail("x@x.com")).thenReturn(Optional.empty());

        service.solicitarRedefinicao("x@x.com");

        verifyNoInteractions(tokenRepository, emailService);
    }

    @Test
    @DisplayName("código correto rotaciona o token e devolve o novo valor")
    void codigoCorreto() {
        PasswordResetToken t = token("123456");
        when(tokenRepository.findAtivosByEmail(eq("ana@x.com"), any())).thenReturn(List.of(t));

        String novo = service.verificarCodigo("ana@x.com", "123456");

        assertThat(novo).isNotBlank().isNotEqualTo("tok-1").isEqualTo(t.getToken());
    }

    @Test
    @DisplayName("código errado conta tentativa; na 5ª o token é invalidado")
    void codigoErrado() {
        PasswordResetToken t = token("123456");
        when(tokenRepository.findAtivosByEmail(eq("ana@x.com"), any())).thenReturn(List.of(t));

        for (int i = 1; i <= 4; i++) {
            assertThatThrownBy(() -> service.verificarCodigo("ana@x.com", "000000"))
                    .isInstanceOf(BusinessException.class);
        }
        assertThat(t.getTentativas()).isEqualTo(4);
        assertThat(t.isUsed()).isFalse();

        assertThatThrownBy(() -> service.verificarCodigo("ana@x.com", "000000"));
        assertThat(t.isUsed()).isTrue();
    }

    @Test
    @DisplayName("sem token ativo, ou código nulo, é inválido")
    void semToken() {
        when(tokenRepository.findAtivosByEmail(eq("ana@x.com"), any()))
                .thenReturn(List.of())
                .thenReturn(List.of(token("123456")));

        assertThatThrownBy(() -> service.verificarCodigo("ana@x.com", "123456"))
                .hasMessageContaining("inválido");
        assertThatThrownBy(() -> service.verificarCodigo("ana@x.com", null))
                .hasMessageContaining("inválido");
    }

    @Test
    @DisplayName("redefine a senha e marca o token como usado")
    void redefine() {
        PasswordResetToken t = token("123456");
        when(tokenRepository.findByToken("tok-1")).thenReturn(Optional.of(t));
        when(passwordEncoder.encode("Nova@123")).thenReturn("hash-novo");

        service.redefinirSenha("tok-1", "Nova@123");

        assertThat(t.getUsuario().getSenha()).isEqualTo("hash-novo");
        assertThat(t.isUsed()).isTrue();
        verify(usuariosRepository).save(t.getUsuario());
    }

    @Test
    @DisplayName("token inexistente, já usado ou expirado não redefine")
    void tokenInvalido() {
        PasswordResetToken usado = token("1");
        usado.setUsed(true);
        PasswordResetToken expirado = token("2");
        expirado.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(tokenRepository.findByToken("nada")).thenReturn(Optional.empty());
        when(tokenRepository.findByToken("usado")).thenReturn(Optional.of(usado));
        when(tokenRepository.findByToken("expirado")).thenReturn(Optional.of(expirado));

        assertThatThrownBy(() -> service.redefinirSenha("nada", "x")).hasMessageContaining("inválido");
        assertThatThrownBy(() -> service.redefinirSenha("usado", "x")).hasMessageContaining("utilizado");
        assertThatThrownBy(() -> service.redefinirSenha("expirado", "x")).hasMessageContaining("expirado");
        verify(usuariosRepository, never()).save(any());
    }
}
