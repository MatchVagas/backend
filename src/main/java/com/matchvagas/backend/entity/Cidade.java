package com.matchvagas.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data

@Entity
@Table(name = "cidades")
public class Cidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "nome", nullable = false,length = 100)
    private String nome;

    @ManyToOne
    @JoinColumn(name = "estado_id", nullable = false)
    private Estado estado;
    
}
