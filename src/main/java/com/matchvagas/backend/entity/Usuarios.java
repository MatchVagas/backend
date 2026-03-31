package com.matchvagas.backend.entity;

import jakarta.persistence.Table;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
@Table(name = "usuarios")
public class Usuarios {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senha;   
    
    @Column(name = "dataNascimento", nullable = false)
    private Date dataNascimento;

    @Column(name = "idade", nullable = false)
    private Integer idade;

    @Column(name = "ativo")
    private Boolean ativo;

    @Column(name = "dataCadastro", nullable = false)
    private Date dataCadastro;

    @Column(name = "dataUltimoAcesso", nullable = false)
    private Date dataUltimoAcesso;

    public Usuarios(String nome, String email, String senha, Date dataNascimento, Integer idade) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.dataNascimento = dataNascimento;
        this.idade = idade;
        this.ativo = true; // Ativo por padrão
        this.dataCadastro = new Date(); // Data de cadastro atual
        this.dataUltimoAcesso = new Date(); // Data do último acesso atual
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }
       

}
