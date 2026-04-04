package com.matchvagas.entity;

import java.util.Set;

import jakarta.persistence.*;
import lombok.Data;

@Data

@Entity
@Table(name = "empresas")
public class Empresas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "cnpj", nullable = false, length = 18)
    private String cnpj;

    @Column(name = "razao_social", length = 150)
    private String razaoSocial;

    @Column(name = "nome_fantasia", length = 150)
    private String nomeFantasia;

    @Column(name = "descricao")
    private String descricao;

    @ManyToOne
    @JoinColumn(name = "porte_id", nullable = false)
    private Porte porte;

    @ManyToOne
    @JoinColumn(name = "ramo_id", nullable = false)
    private RamoAtuacao ramoAtuacao;

    @Column(name = "site", length = 150)
    private String site;

    @ManyToMany
    @JoinTable(
        name = "telefones_empresa",
        joinColumns = @JoinColumn(name = "empresa_id"),
        inverseJoinColumns = @JoinColumn(name = "telefone_id")
    )
    private Set<Telefones> telefones;

    
}
