package com.matchvagas.backend.entity;

import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import jakarta.persistence.JoinColumn;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
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

    @Column(name = "senha_hash", nullable = false, columnDefinition = "TEXT")
    private String senha;

    @Column(name = "dataNascimento", nullable = false)
    private Date dataNascimento;

    @Column(name = "idade", nullable = false)
    private Integer idade;

    @Column(name = "ativo")
    private Boolean ativo;

    @Column(name = "dataCadastro", nullable = false)
    private LocalDateTime dataCadastro;

    @Column(name = "dataUltimoAcesso", nullable = false)
    private LocalDateTime dataUltimoAcesso;

    @ManyToMany(cascade = {jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.MERGE})
    @JoinTable(name = "telefones_usuario", 
        joinColumns = @JoinColumn(name = "usuario_id"), 
        inverseJoinColumns = @JoinColumn(name = "telefone_id"))
    private List<Telefones> telefones;

    @PrePersist
    protected void onCreate() {
        this.dataCadastro = LocalDateTime.now();
        this.ativo = true;
    }

}
