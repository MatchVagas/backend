package com.matchvagas.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(length = 6)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuarios usuario;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    // SEC: nº de tentativas de código erradas. Acima do limite o token é
    // invalidado, impedindo brute force do código de 6 dígitos.
    @Column(nullable = false)
    private int tentativas = 0;

    @PrePersist
    protected void onCreate() {
        if (this.expiresAt == null) {
            this.expiresAt = LocalDateTime.now().plusHours(1);
        }
    }
}
