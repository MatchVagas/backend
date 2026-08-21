package com.matchvagas.backend.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "curriculo")
public class Curriculos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Referência ao candidato dono deste currículo
    @ManyToOne
    @JoinColumn(name = "candidato_id", referencedColumnName = "id")
    private Candidatos candidato;

    @Column(name = "nome_arquivo", nullable = false, length = 255)
    private String nomeArquivo;

    @Column(name = "caminho_arquivo", columnDefinition = "TEXT")
    private String caminhoArquivo;

    @Column(name = "data_upload")
    private LocalDateTime dataUpload;

    @Column(name = "tamanho_arquivo")
    private BigInteger tamanhoArquivo;

    @Column(name = "formato_arquivo", length = 50)
    private String formatoArquivo;

    @Column(name = "texto_extraido", columnDefinition = "TEXT")
    private String textoExtraido;

    @Column(name = "dados_estruturados", columnDefinition = "TEXT")
    private String dadosEstruturados;

    @Column(name = "parseado_em")
    private LocalDateTime parseadoEm;

    @PrePersist
    protected void onCreate() {
        this.dataUpload = LocalDateTime.now();
    }
}
