package com.matchvagas.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Trilha de auditoria de acesso a dados pessoais (LGPD Art. 37 — registro das
 * operações de tratamento). Registra quem acessou qual recurso, quando e de onde.
 */
@Data
@Entity
@Table(name = "auditoria_acesso", indexes = {
        @Index(name = "idx_auditoria_recurso", columnList = "tipo_recurso, recurso_id"),
        @Index(name = "idx_auditoria_solicitante", columnList = "usuario_solicitante_id")
})
public class AuditoriaAcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_solicitante_id")
    private Long usuarioSolicitanteId;

    @Column(name = "recurso_id")
    private Long recursoId;

    @Column(name = "tipo_recurso", length = 40)
    private String tipoRecurso;   // ex: "CURRICULO", "CANDIDATO", "EMPRESA"

    @Column(name = "operacao", length = 20)
    private String operacao;      // ex: "READ", "UPDATE", "DELETE", "EXPORT"

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "ip_origem", length = 64)
    private String ipOrigem;

    @PrePersist
    void onCreate() {
        if (this.dataHora == null) {
            this.dataHora = LocalDateTime.now();
        }
    }
}
