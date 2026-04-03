package com.matchvagas.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;

@Entity
@Table(name = "endereco")
public class Endereco {
    
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@Column(name = "logradouro", nullable = false)
private String logradouro;

@Column(name = "numero", nullable = false)
private String numero;

@Column(name = "completo", nullable = false)
private String completo;

@Column(name = "estado", nullable = false)
private String estado;

@Column(name = "cidade", nullable = false)
private String cidade;

@Column(name = "bairro", nullable = false)
private String bairro;

@Column(name = "cep", nullable = false)
private String cep;
 public Endereco(String logradouro, String numero, String completo, String estado, String cidade, String bairro, String cep){
    this.logradouro = logradouro;
    this.numero = numero;
    this.completo = completo;
    this.estado = estado;
    this.cidade = cidade;
    this.bairro = bairro;
    this.cep = cep;
 }

  // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
}