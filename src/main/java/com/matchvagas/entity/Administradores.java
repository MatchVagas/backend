package com.matchvagas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data

@Entity
@Table(name = "administradores")
public class Administradores {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "usuarios_id", nullable = false)
    private Usuarios usuario;

    @Column(name = "nivel", nullable = false, length = 50)
    private String nivel;

    @ManyToOne
    @JoinColumn(name = "departamento_id", nullable = false)
    private Departamentos departamento;

    @Column(name = "permissoes", nullable = false, columnDefinition = "TEXT")
    private String permissoes;
    
}
