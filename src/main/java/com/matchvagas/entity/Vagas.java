package com.matchvagas.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data

@Entity
@Table(name = "vagas")
public class Vagas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "empresa_id", nullable = false)
    private Empresas empresas;

    @Column(name = "titulo", nullable = false,length = 255)
    private String titulo;

    @Column(name = "descricao", nullable = false, columnDefinition = "TEXT")
    private String descricao;   

    @Column(name = "requisitos", nullable = false, columnDefinition = "TEXT")
    private String requisitos;

    @Column(name = "tipo_vaga_id", nullable = false)
    private TipoVaga tipoVaga;

    @Column(name = "modalidade_vaga_id", nullable = false)
    private Modalidade modalidade;

    @Column(name = "salario_min", nullable = false, precision = 10, scale = 2)
    private BigDecimal salarioMinimo;

    @Column(name = "salario_max", nullable = false, precision = 10, scale = 2)
    private BigDecimal salarioMaximo;

    @Column(name = "beneficios", nullable = true, columnDefinition = "TEXT")
    private String beneficios;

    
    
}
