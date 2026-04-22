package com.matchvagas.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data

@Entity
@Table(name = "formacoes")
public class Formacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instituicao", nullable = false)
    private String instituicao;

    @Column(name = "curso", nullable = false)
    private String curso;

    @Column(name = "nivel", nullable = false)
    private String nivel; 

    @Column(name = "data_inicio", nullable = false)
    private String dataInicio; 

    @Column(name = "data_fim")
    private String dataFim; 
    // Relacionamento: cada formação pertence a um candidato
    @ManyToOne
    @JoinColumn(name = "candidato_id", nullable = false)
    private Candidatos candidato;

}
