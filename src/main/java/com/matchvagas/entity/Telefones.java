package com.matchvagas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "telefones")
public class Telefones {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "numero", nullable = false)
  private String numero;

  @Column(name = "tipo_telefones", nullable = false)
  private String tipo_telefones;

  @Column(name = "wpp", nullable = false)
  private String wpp;

  public Telefones(String numero, String tipo_telefones , String wpp) {
    this.numero = numero;
    this.tipo_telefones = tipo_telefones;
    this.wpp = wpp;


 }
  // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public String getNumero() {
        return numero;
    }

    public void setTipo_Telefones(String tipo_telefones) {
        this.tipo_telefones = tipo_telefones;
    }
  



}