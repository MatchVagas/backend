package com.matchvagas.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data

@Entity
@Table(name = "vagas")
public class Vagas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresas empresas;

    @Column(name = "titulo", nullable = false,length = 255)
    private String titulo;

    @Column(name = "descricao", nullable = false, columnDefinition = "TEXT")
    private String descricao;   

    @Column(name = "requisitos", nullable = false, columnDefinition = "TEXT")
    private String requisitos;

    @OneToOne
    @JoinColumn(name = "tipo_vaga_id", nullable = false)
    private TipoVaga tipoVaga;

    @OneToOne
    @JoinColumn(name = "modalidade_id", nullable = false)
    private Modalidade modalidade;

    @Column(name = "salario_min", nullable = false, precision = 10, scale = 2)
    private BigDecimal salarioMinimo;

    @Column(name = "salario_max", nullable = false, precision = 10, scale = 2)
    private BigDecimal salarioMaximo;

    @Column(name = "beneficios", nullable = true, columnDefinition = "TEXT")
    private String beneficios;

    @Column(name = "carga_horaria", nullable = false, length = 50)
    private String cargaHoraria;

    @Column(name = "idade_minima", nullable = false)
    private int idadeMinima;

    @Column(name = "idade_maxima", nullable = false)
    private int idadeMaxima;

    @OneToOne
    @JoinColumn(name = "nivel_escolaridade_minimo_id", nullable = false)
    private Escolaridades escolaridade;

    @Column(name = "area_atuacao", nullable = false, length = 100)
    private String areaAtuacao;

    @Column(name = "data_publicacao", nullable = false)
    private LocalDateTime dataPublicacao;

    @Column(name = "data_expiracao", nullable = false)
    private LocalDateTime dataExpiracao;

    @OneToOne
    @JoinColumn(name = "status_vaga_id", nullable = false)
    private StatusVaga status;

    @Column(name = "numero_vagas", nullable = false)
    private int numeroVagas;

    @OneToOne
    @JoinColumn(name = "cidade_id", nullable = false)
    private Cidade cidade;
    
}
