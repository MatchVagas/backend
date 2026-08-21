package com.matchvagas.backend.service;

import com.matchvagas.backend.entity.RefreshToken;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Emissão, rotação e revogação de refresh tokens (Fase 1). A rotação (invalidar
 * o token usado ao renovar) limita a janela de reuso de um token vazado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    // Validade do refresh token em ms (padrão 7 dias). O access token (JWT) é curto.
    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs;

    /** Cria e persiste um novo refresh token para o usuário. */
    @Transactional
    public String gerar(Usuarios usuario) {
        RefreshToken rt = new RefreshToken();
        rt.setToken(UUID.randomUUID().toString());
        rt.setUsuario(usuario);
        rt.setExpiresAt(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000));
        refreshTokenRepository.save(rt);
        return rt.getToken();
    }

    /**
     * Valida um refresh token e o rotaciona (marca como revogado). Retorna o
     * registro para que o chamador emita novos tokens ao usuário associado.
     */
    @Transactional
    public RefreshToken validarERotacionar(String tokenValor) {
        RefreshToken rt = refreshTokenRepository.findByToken(tokenValor)
                .orElseThrow(() -> new BusinessException("Refresh token inválido."));

        if (rt.isRevoked()) {
            throw new BusinessException("Sessão revogada. Faça login novamente.");
        }
        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Sessão expirada. Faça login novamente.");
        }

        rt.setRevoked(true);
        refreshTokenRepository.save(rt);
        return rt;
    }

    /** Revoga um refresh token específico (logout). Silencioso se não existir. */
    @Transactional
    public void revogar(String tokenValor) {
        refreshTokenRepository.findByToken(tokenValor).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    /** Revoga todas as sessões ativas de um usuário. */
    @Transactional
    public void revogarSessoes(Long usuarioId) {
        refreshTokenRepository.revogarTodosDoUsuario(usuarioId);
    }
}
