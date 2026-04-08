package com.matchvagas.backend.entity;

import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data

@Entity
@Table(name = "empresas")
public class Empresas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @ManyToMany(cascade = {jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.MERGE})
    @JoinTable(
        name = "telefones_empresa",
        joinColumns = @JoinColumn(name = "empresa_id"),
        inverseJoinColumns = @JoinColumn(name = "telefone_id")
    )
    private List<Telefones> telefones;

    
}
