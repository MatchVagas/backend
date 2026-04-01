package com.matchvagas.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data

@Entity
@Table(name = "empresas")
public class Empresas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "cnpj", nullable = false)
    private String cnpj;

    @Column(name = "razao_social")
    private String razao_Social;

    @Column(name = "nome_fantasia")
    private String nome_fantasia;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "porte_id")
    private int porte_id;

    @Column(name = "ramo_id")
    private int ramo_id;

   
    @Column(name = "site")
    private String site;

    
}
