package com.matchvagas.backend.entity;

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

    @Column(name = "cpf", nullable = false, unique = true, length = 20)
    private String cpf;

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
}
