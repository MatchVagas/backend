package com.matchvagas.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;

import java.math.BigDecimal;

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

    @Column(name = "objetivo_profissional", columnDefinition = "TEXT")
    private String objetivoProfissional;

    @Column(name = "pretensao_salarial", precision = 12, scale = 2)
    private BigDecimal pretensaoSalarial;

    @Column(name = "disponibilidade", length = 100)
    private String disponibilidade;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "usuario_id", referencedColumnName = "id")
    private Usuarios usuario;
}
