package com.matchvagas.backend.entity;

import jakarta.persistence.*;

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

    // Relacionamento: cada experiência pertence a um candidato
    @ManyToOne
    @JoinColumn(name = "candidato_id", nullable = false)
    private CandidatoVaga candidato;

    public Experiencia() {}

    public Experiencia(String empresa, String cargo, String descricao, String dataInicio, String dataFim, CandidatoVaga candidato) {
        this.empresa = empresa;
        this.cargo = cargo;
        this.descricao = descricao;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.candidato = candidato;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmpresa() { return empresa; }
    public void setEmpresa(String empresa) { this.empresa = empresa; }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getDataInicio() { return dataInicio; }
    public void setDataInicio(String dataInicio) { this.dataInicio = dataInicio; }

    public String getDataFim() { return dataFim; }
    public void setDataFim(String dataFim) { this.dataFim = dataFim; }

    public CandidatoVaga getCandidato() { return candidato; }
    public void setCandidato(CandidatoVaga candidato) { this.candidato = candidato; }
}
