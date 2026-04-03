package com.matchvagas.entity;

import jakarta.persistence.*;
import lombok.Data;
import com.matchvagas.entity.Vaga;


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
    private Vaga vaga;

    @Column(name = "status", nullable = false)
    private String status; // Ex: "Em análise", "Aprovado", "Rejeitado"

    @Column(name = "data_candidatura", nullable = false)
    private String dataCandidatura; // poderia ser LocalDate

}
