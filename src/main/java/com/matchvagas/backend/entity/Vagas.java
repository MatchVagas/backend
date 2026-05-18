package com.matchvagas.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "vagas")
public class Vagas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // CORRIGIDO: @ManyToOne — uma empresa pode ter muitas vagas
    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresas empresas;

    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    @Column(name = "descricao", nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "requisitos", nullable = false, columnDefinition = "TEXT")
    private String requisitos;

    // CORRIGIDO: @ManyToOne — lookup table compartilhada entre muitas vagas
    @ManyToOne
    @JoinColumn(name = "tipo_vaga_id", nullable = false)
    private TipoVaga tipoVaga;

    // CORRIGIDO: @ManyToOne
    @ManyToOne
    @JoinColumn(name = "modalidade_id", nullable = false)
    private Modalidade modalidade;

    @Column(name = "salario_min", nullable = false, precision = 10, scale = 2)
    private BigDecimal salarioMinimo;

    @Column(name = "salario_max", nullable = false, precision = 10, scale = 2)
    private BigDecimal salarioMaximo;

    @Column(name = "beneficios", columnDefinition = "TEXT")
    private String beneficios;

    @Column(name = "carga_horaria", nullable = false, length = 50)
    private String cargaHoraria;

    @Column(name = "idade_minima")
    private Integer idadeMinima;

    @Column(name = "idade_maxima")
    private Integer idadeMaxima;

    // CORRIGIDO: @ManyToOne
    @ManyToOne
    @JoinColumn(name = "nivel_escolaridade_minimo_id", nullable = false)
    private Escolaridades escolaridade;

    @Column(name = "area_atuacao", nullable = false, length = 100)
    private String areaAtuacao;

    @Column(name = "data_publicacao", nullable = false)
    private LocalDateTime dataPublicacao;

    @Column(name = "data_expiracao", nullable = false)
    private LocalDateTime dataExpiracao;

    // CORRIGIDO: @ManyToOne
    @ManyToOne
    @JoinColumn(name = "status_vaga_id", nullable = false)
    private StatusVaga status;

    @Column(name = "numero_vagas", nullable = false)
    private int numeroVagas;

    // CORRIGIDO: @ManyToOne
    @ManyToOne
    @JoinColumn(name = "cidade_id", nullable = false)
    private Cidade cidade;

    @PrePersist
    protected void onCreate() {
        this.dataPublicacao = LocalDateTime.now();
    }
}
