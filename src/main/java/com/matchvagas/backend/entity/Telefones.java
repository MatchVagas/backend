package com.matchvagas.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "telefones")
public class Telefones {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false, length = 20, unique = true)
    private String numero;

    @ManyToOne
    @JoinColumn(name = "tipo_telefone_id", nullable = false)
    private TipoTelefone tipoTelefone;

    @Column(name = "wpp", nullable = false)
    private boolean wpp = false;

    // ====================== RELACIONAMENTOS MANY TO MANY ======================

    /**
     * Telefones podem pertencer a várias Empresas
     */
    @ManyToMany(mappedBy = "telefones")
    private List<Empresas> empresas = new ArrayList<>();

    /**
     * Telefones podem pertencer a vários Usuários (candidatos)
     */
    @ManyToMany(mappedBy = "telefones")
    private List<Usuarios> usuarios = new ArrayList<>();

    // ====================== MÉTODOS AUXILIARES (Helpers) ======================

    /**
     * Adiciona esta instância de telefone a uma empresa
     */
    public void adicionarEmpresa(Empresas empresa) {
        this.empresas.add(empresa);
        empresa.getTelefones().add(this);   // Mantém sincronia bidirecional
    }

    /**
     * Remove esta instância de telefone de uma empresa
     */
    public void removerEmpresa(Empresas empresa) {
        this.empresas.remove(empresa);
        empresa.getTelefones().remove(this);
    }

    /**
     * Adiciona esta instância de telefone a um usuário
     */
    public void adicionarUsuario(Usuarios usuario) {
        this.usuarios.add(usuario);
        usuario.getTelefones().add(this);
    }

    /**
     * Remove esta instância de telefone de um usuário
     */
    public void removerUsuario(Usuarios usuario) {
        this.usuarios.remove(usuario);
        usuario.getTelefones().remove(this);
    }

}