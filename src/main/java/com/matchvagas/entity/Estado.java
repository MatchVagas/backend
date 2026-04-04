package com.matchvagas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "estados")
public class Estado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "nome", nullable = false,length = 100)
    private String nome;

    @Column(name = "uf", nullable = false,length = 2)
    private String uf;

    @ManyToOne
    @JoinColumn(name = "pais_id")
    private Pais pais;
    
}
