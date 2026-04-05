package com.matchvagas.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;


@Data

@Entity
@Table(name = "candidaturas")
public class Candidatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relacionamento: uma candidatura pertence a um candidato
    @ManyToOne
    @JoinColumn(name = "candidato_id", nullable = false)
    private CandidatoVaga candidato;

    // Relacionamento: uma candidatura pertence a uma vaga
    @ManyToOne
    @JoinColumn(name = "vaga_id", nullable = false)
    private Vagas vaga;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "status_id", referencedColumnName = "id")
    private StatusCandidatura status; // Ex: "Em análise", "Aprovado", "Rejeitado"

    @Column(name = "data_candidatura", nullable = false)
    private LocalDateTime dataCandidatura; // poderia ser LocalDate

}
