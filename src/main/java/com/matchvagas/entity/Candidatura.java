package com.matchvagas.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "candidaturas")
public class Candidatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relacionamento: uma candidatura pertence a um candidato
    @ManyToOne
    @JoinColumn(name = "candidato_id", nullable = false)
    private CandidatoVaga candidato;

    // Relacionamento: uma candidatura pertence a uma vaga
    @ManyToOne
    @JoinColumn(name = "vaga_id", nullable = false)
    private Vaga vaga;

    @Column(name = "status", nullable = false)
    private String status; // Ex: "Em análise", "Aprovado", "Rejeitado"

    @Column(name = "data_candidatura", nullable = false)
    private String dataCandidatura; // poderia ser LocalDate

    public Candidatura() {}

    public Candidatura(CandidatoVaga candidato, Vaga vaga, String status, String dataCandidatura) {
        this.candidato = candidato;
        this.vaga = vaga;
        this.status = status;
        this.dataCandidatura = dataCandidatura;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CandidatoVaga getCandidato() { return candidato; }
    public void setCandidato(CandidatoVaga candidato) { this.candidato = candidato; }

    public Vaga getVaga() { return vaga; }
    public void setVaga(Vaga vaga) { this.vaga = vaga; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDataCandidatura() { return dataCandidatura; }
    public void setDataCandidatura(String dataCandidatura) { this.dataCandidatura = dataCandidatura; }
}
