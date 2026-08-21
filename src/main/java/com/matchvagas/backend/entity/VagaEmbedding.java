package com.matchvagas.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "vaga_embedding")
public class VagaEmbedding {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "vaga_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Vagas vaga;

    @Column(name = "vetor", nullable = false, columnDefinition = "TEXT")
    private String vetor;
    @Column(name = "modelo", nullable = false, length = 100)
    private String modelo;
    @Column(name = "dim", nullable = false)
    private Integer dim;
    @Column(name = "texto_hash", nullable = false, length = 64)
    private String textoHash;
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist @PreUpdate
    void touch() { atualizadoEm = LocalDateTime.now(); }
}
