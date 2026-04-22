package com.matchvagas.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "experiencias")
public class Experiencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa", nullable = false)
    private String empresa;

    @Column(name = "cargo", nullable = false)
    private String cargo;

    @Column(name = "descricao", length = 1000)
    private String descricao;

    @Column(name = "data_inicio", nullable = false)
    private String dataInicio;

    @Column(name = "data_fim")
    private String dataFim;

    @ManyToOne
    @JoinColumn(name = "candidato_id", nullable = false)
    private Candidatos candidato;
}
