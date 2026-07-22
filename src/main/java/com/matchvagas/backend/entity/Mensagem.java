package com.matchvagas.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Mensagem trocada entre a empresa e o candidato no contexto de uma candidatura
 * (Fase 2 — comunicação empresa ↔ candidato). Cada candidatura funciona como uma
 * conversa (thread); os participantes são o candidato dono da candidatura e o
 * usuário gestor da empresa dona da vaga.
 */
@Data
@Entity
@Table(name = "mensagens",
    indexes = @Index(name = "idx_mensagem_candidatura", columnList = "candidatura_id"))
public class Mensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Conversa a que a mensagem pertence (contexto e regra de acesso).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidatura_id", nullable = false)
    private Candidatura candidatura;

    // Quem enviou. O "papel" (candidato/empresa) é derivado comparando este
    // usuário com o dono da candidatura, sem duplicar o dado.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "remetente_id", nullable = false)
    private Usuarios remetente;

    @Column(name = "conteudo", nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    @Column(name = "data_envio", nullable = false)
    private LocalDateTime dataEnvio;

    // Lida pelo destinatário (a outra parte da conversa).
    @Column(name = "lida", nullable = false)
    private boolean lida = false;

    @PrePersist
    protected void onCreate() {
        this.dataEnvio = LocalDateTime.now();
    }
}
