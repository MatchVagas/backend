package com.matchvagas.backend.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "candidaturas",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_candidatura_candidato_vaga",
        columnNames = {"candidato_id", "vaga_id"}
    )
)
public class Candidatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "candidato_id", nullable = false)
    private Candidatos candidato;

    @ManyToOne
    @JoinColumn(name = "vaga_id", nullable = false)
    private Vagas vaga;

    // CORRIGIDO: @ManyToOne — status é uma lookup table compartilhada
    @ManyToOne
    @JoinColumn(name = "status_id")
    private StatusCandidatura status;

    @Column(name = "data_candidatura", nullable = false)
    private LocalDateTime dataCandidatura;

    @PrePersist
    protected void onCreate() {
        this.dataCandidatura = LocalDateTime.now();
    }
}
