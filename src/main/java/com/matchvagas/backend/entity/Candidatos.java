package com.matchvagas.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;


@Entity
@Table(name = "candidatos_vaga")
public class CandidatoVaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "vaga", nullable = false)
    private String vaga;

    @Column(name = "formacao")
    private String formacao;

    @Column(name = "experiencia")
    private String experiencia;

    // Construtor padrão
    public CandidatoVaga() {}

    // Construtor com parâmetros
    public CandidatoVaga(String nome, String email, String vaga, String formacao, String experiencia) {
        this.nome = nome;
        this.email = email;
        this.vaga = vaga;
        this.formacao = formacao;
        this.experiencia = experiencia;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getVaga() { return vaga; }
    public void setVaga(String vaga) { this.vaga = vaga; }

    public String getFormacao() { return formacao; }
    public void setFormacao(String formacao) { this.formacao = formacao; }

    public String getExperiencia() { return experiencia; }
    public void setExperiencia(String experiencia) { this.experiencia = experiencia; }

    public List<Telefones> getTelefones() { return telefones; }
    public void setTelefones(List<Telefones> telefones) { this.telefones = telefones; }
}
