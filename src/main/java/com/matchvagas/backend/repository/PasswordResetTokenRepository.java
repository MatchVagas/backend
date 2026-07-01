package com.matchvagas.backend.repository;

import com.matchvagas.backend.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    @Query("SELECT t FROM PasswordResetToken t WHERE t.usuario.email = :email AND t.codigo = :codigo AND t.used = false AND t.expiresAt > :now")
    Optional<PasswordResetToken> findValidByCodigo(String email, String codigo, LocalDateTime now);

    // Token ativo (não usado e não expirado) mais recente do e-mail. Usado na
    // verificação de código para contar tentativas mesmo quando o código erra.
    @Query("SELECT t FROM PasswordResetToken t WHERE t.usuario.email = :email AND t.used = false AND t.expiresAt > :now ORDER BY t.id DESC")
    java.util.List<PasswordResetToken> findAtivosByEmail(String email, LocalDateTime now);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :now OR t.used = true")
    void deleteExpiredAndUsed(LocalDateTime now);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.usuario.id = :usuarioId")
    void deleteByUsuarioId(Long usuarioId);
}
