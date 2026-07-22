package com.matchvagas.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Alerta de vaga por critérios criado por um candidato (Fase 2). Quando uma vaga ATIVA
 * é publicada e casa com os critérios de um alerta ativo, o candidato é notificado.
 *
 * <p>Todos os critérios são opcionais; {@code null} significa "qualquer". Um alerta sem
 * nenhum critério casa com toda vaga nova.
 */
@Data
@Entity
@Table(name = "alertas_vaga")
public class AlertaVaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "candidato_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Candidatos candidato;

    @Column(name = "area_atuacao", length = 100)
    private String areaAtuacao;

    @Column(name = "cidade_id")
    private Long cidadeId;

    @Column(name = "modalidade_id")
    private Long modalidadeId;

    @Column(name = "salario_minimo_desejado", precision = 12, scale = 2)
    private BigDecimal salarioMinimoDesejado;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    protected void onCreate() {
        this.dataCriacao = LocalDateTime.now();
    }
}
