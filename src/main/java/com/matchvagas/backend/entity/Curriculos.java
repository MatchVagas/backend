package com.matchvagas.backend.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "curriculo")
public class Curriculos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "candidato_id", referencedColumnName = "id")
    private Candidatos candidato;

    @Column(name = "nome_arquivo", nullable = false, length = 255)
    private String nome_arquivo;

    @Column(name = "caminho_arquivo", columnDefinition = "TEXT")
    private String caminho_arquivo;

    @Column(name = "data_upload")
    private LocalDateTime data_upload;

    @Column(name = "tamanho_arquivo")
    private BigInteger tamanho_arquivo;

    @Column(name = "formato_arquivo",length = 50)
    private String formato_arquivo;

    @PrePersist
    protected void onCreate() {
        this.data_upload = LocalDateTime.now();
    }
    
}
