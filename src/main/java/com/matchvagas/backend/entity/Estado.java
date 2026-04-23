package com.matchvagas.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "estados")
public class Estado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "uf", nullable = false, length = 2)
    private String uf;

    @ManyToOne
    @JoinColumn(name = "pais_id")
    private Pais pais;
}
