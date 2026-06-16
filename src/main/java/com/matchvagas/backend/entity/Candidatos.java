package com.matchvagas.backend.entity;

import com.matchvagas.backend.util.CpfCrypto;
import com.matchvagas.backend.util.CpfCryptoConverter;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data


@Entity
@Table(name = "candidatos")
public class Candidatos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LGPD-04 — CPF cifrado (AES-GCM) em repouso. O valor cifrado é maior que o
    // CPF original e não é determinístico, por isso a unicidade é garantida pelo
    // hash determinístico em cpfHash (não mais por unique nesta coluna).
    @Convert(converter = CpfCryptoConverter.class)
    @Column(name = "cpf", nullable = false, length = 255)
    private String cpf;

    // Hash HMAC-SHA256 determinístico do CPF, usado para checar unicidade.
    @Column(name = "cpf_hash", length = 64, unique = true)
    private String cpfHash;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "endereco_id", referencedColumnName = "id")
    private Endereco endereco;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "curriculo_id", referencedColumnName = "id") 
    private Curriculos curriculo;

    @Column(name = "objetivo_profissional", columnDefinition = "TEXT")
    private String objetivoProfissional;

    @Column(name = "pretensao_salarial", precision = 12, scale = 2)
    private BigDecimal pretensaoSalarial;

    @Column(name = "disponibilidade", length = 100)
    private String disponibilidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "genero", length = 30)
    private Genero genero;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "usuario_id", referencedColumnName = "id")
    private Usuarios usuario;

    @Column(name = "foto_perfil_url", columnDefinition = "TEXT")
    private String fotoPerfilUrl;

    @ElementCollection
    @CollectionTable(name = "candidato_habilidades", joinColumns = @JoinColumn(name = "candidato_id"))
    private List<Habilidade> habilidades = new ArrayList<>();

    // Mantém o hash do CPF sincronizado com o valor (em texto puro) na memória.
    @PrePersist
    @PreUpdate
    void sincronizarCpfHash() {
        this.cpfHash = (cpf == null || cpf.isBlank()) ? null : CpfCrypto.hash(cpf);
    }
}
