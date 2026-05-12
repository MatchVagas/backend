package com.matchvagas.backend.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "candidaturas",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_candidatura_candidato_vaga",
        columnNames = {"candidato_id", "vaga_id"}
    )
)
public class Candidatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "candidato_id", nullable = false)
    private Candidatos candidato;

    @ManyToOne
    @JoinColumn(name = "vaga_id", nullable = false)
    private Vagas vaga;

    @ManyToOne
    @JoinColumn(name = "status_id")
    private StatusCandidatura status;

    @Column(name = "data_candidatura", nullable = false)
    private LocalDateTime dataCandidatura;

    // ── Preferências de compartilhamento com a empresa ───────────────────────
    // Dados profissionais: compartilhados por padrão
    @Column(name = "compartilhar_objetivo", nullable = false)
    private boolean compartilharObjetivoProfissional = true;

    @Column(name = "compartilhar_disponibilidade", nullable = false)
    private boolean compartilharDisponibilidade = true;

    @Column(name = "compartilhar_pretensao_salarial", nullable = false)
    private boolean compartilharPretensaoSalarial = true;

    @Column(name = "compartilhar_curriculo", nullable = false)
    private boolean compartilharCurriculo = true;

    @Column(name = "compartilhar_experiencias", nullable = false)
    private boolean compartilharExperiencias = true;

    @Column(name = "compartilhar_formacoes", nullable = false)
    private boolean compartilharFormacoes = true;

    // Dados pessoais de contato: privados por padrão
    @Column(name = "compartilhar_telefone", nullable = false)
    private boolean compartilharTelefone = false;

    @Column(name = "compartilhar_endereco", nullable = false)
    private boolean compartilharEndereco = false;

    @PrePersist
    protected void onCreate() {
        this.dataCandidatura = LocalDateTime.now();
    }
}
