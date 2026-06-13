package com.matchvagas.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "historico_status_candidatura")
public class HistoricoStatusCandidatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidatura_id", nullable = false)
    private Candidatura candidatura;

    // Status de origem — nulo quando a candidatura ainda não tinha status definido
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_anterior_id")
    private StatusCandidatura statusAnterior;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_novo_id", nullable = false)
    private StatusCandidatura statusNovo;

    // Usuário (recrutador/empresa) que efetuou a mudança
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuarios usuario;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @PrePersist
    protected void onCreate() {
        this.dataHora = LocalDateTime.now();
    }
}
