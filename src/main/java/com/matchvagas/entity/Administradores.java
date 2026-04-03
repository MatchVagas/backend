package com.matchvagas.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "administradores")
public class Administradores {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private int usuarios_id;
    private String nivel;
    private int departamento_id;
    private String permissoes;
    
}
