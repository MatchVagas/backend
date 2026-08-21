package com.matchvagas.backend.service;

import com.matchvagas.backend.entity.RefreshToken;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fase 1 — Refresh token")
class RefreshTokenServiceTest {

    @Mock RefreshTokenRepository refreshTokenRepository;

    @InjectMocks RefreshTokenService service;

    private Usuarios usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuarios();
        usuario.setId(1L);
        usuario.setEmail("joao@email.com");
        // @Value não é injetado no teste unitário — define a validade manualmente.
        ReflectionTestUtils.setField(service, "refreshExpirationMs", 604800000L);
    }

    private RefreshToken token(boolean revoked, LocalDateTime expiresAt) {
        RefreshToken rt = new RefreshToken();
        rt.setToken("rt-1");
        rt.setUsuario(usuario);
        rt.setRevoked(revoked);
        rt.setExpiresAt(expiresAt);
        return rt;
    }

    @Test
    @DisplayName("gerar cria token com validade futura e persiste")
    void gerarPersiste() {
        String valor = service.gerar(usuario);

        assertThat(valor).isNotBlank();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("validarERotacionar aceita token válido e o revoga (rotação)")
    void validaERotaciona() {
        RefreshToken rt = token(false, LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByToken("rt-1")).thenReturn(Optional.of(rt));

        RefreshToken resultado = service.validarERotacionar("rt-1");

        assertThat(resultado.isRevoked()).isTrue();
        assertThat(resultado.getUsuario()).isEqualTo(usuario);
        verify(refreshTokenRepository).save(rt);
    }

    @Test
    @DisplayName("validarERotacionar rejeita token inexistente")
    void rejeitaInexistente() {
        when(refreshTokenRepository.findByToken("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validarERotacionar("x"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inválido");
    }

    @Test
    @DisplayName("validarERotacionar rejeita token revogado")
    void rejeitaRevogado() {
        when(refreshTokenRepository.findByToken("rt-1"))
                .thenReturn(Optional.of(token(true, LocalDateTime.now().plusDays(1))));

        assertThatThrownBy(() -> service.validarERotacionar("rt-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("revogada");
    }

    @Test
    @DisplayName("validarERotacionar rejeita token expirado")
    void rejeitaExpirado() {
        when(refreshTokenRepository.findByToken("rt-1"))
                .thenReturn(Optional.of(token(false, LocalDateTime.now().minusMinutes(1))));

        assertThatThrownBy(() -> service.validarERotacionar("rt-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expirada");
    }

    @Test
    @DisplayName("revogar marca o token como revogado")
    void revoga() {
        RefreshToken rt = token(false, LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByToken("rt-1")).thenReturn(Optional.of(rt));

        service.revogar("rt-1");

        assertThat(rt.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(rt);
    }

    @Test
    @DisplayName("revogar é silencioso para token inexistente")
    void revogaInexistenteSilencioso() {
        when(refreshTokenRepository.findByToken("x")).thenReturn(Optional.empty());

        service.revogar("x");

        verify(refreshTokenRepository, never()).save(any());
    }
}
