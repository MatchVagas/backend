package com.matchvagas.backend.repository;

import com.matchvagas.backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    // Revoga todas as sessões ativas de um usuário (ex.: logout global, troca de senha).
    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.usuario.id = :usuarioId AND t.revoked = false")
    void revogarTodosDoUsuario(Long usuarioId);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :now OR t.revoked = true")
    void deleteExpiredAndRevoked(LocalDateTime now);
}
