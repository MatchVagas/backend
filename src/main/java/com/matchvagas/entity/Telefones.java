package com.matchvagas.entity;

import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data

@Entity
@Table(name = "telefones")
public class Telefones {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false)
    private String numero;

    @OneToOne
    @JoinColumn(name = "tipo_telefone", nullable = false)
    private TipoTelefone tipoTelefone;

    @ManyToMany (mappedBy = "telefones")
    private Set<Usuarios> usuarios;

    @ManyToMany(mappedBy = "telefones")
    private Set<Empresas> empresas;

    @Column(name = "wpp", nullable = false)
    private boolean wpp;

}