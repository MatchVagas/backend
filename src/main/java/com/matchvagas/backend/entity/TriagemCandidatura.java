package com.matchvagas.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/** Triagem assistida por IA de uma candidatura. Gerada uma vez e reaproveitada enquanto a entrada não mudar. */
@Data
@Entity
@Table(name = "triagem_candidatura")
public class TriagemCandidatura {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "candidatura_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Candidatura candidatura;

    @Column(name = "parecer", nullable = false, columnDefinition = "TEXT")
    private String parecer;
    // Um item por linha.
    @Column(name = "pontos_fortes", columnDefinition = "TEXT")
    private String pontosFortes;
    @Column(name = "lacunas", columnDefinition = "TEXT")
    private String lacunas;
    @Enumerated(EnumType.STRING)
    @Column(name = "recomendacao", nullable = false, length = 20)
    private RecomendacaoTriagem recomendacao;
    @Column(name = "modelo", nullable = false, length = 100)
    private String modelo;
    @Column(name = "entrada_hash", nullable = false, length = 64)
    private String entradaHash;
    @Column(name = "gerado_por_usuario_id")
    private Long geradoPorUsuarioId;
    @Column(name = "gerado_em", nullable = false)
    private LocalDateTime geradoEm;
}