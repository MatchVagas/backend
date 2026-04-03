package com.matchvagas.entity;

import jakarta.persistence.Table;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

@Data

@Entity
@Table(name = "usuarios")
public class Usuarios {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private int id;

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


}
