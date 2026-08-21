package com.matchvagas.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "usuarios")
public class Usuarios {

    // Versão vigente da política de privacidade aceita no cadastro (LGPD Art. 7º/8º)
    public static final String VERSAO_POLITICA_PRIVACIDADE_ATUAL = "1.0";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false, columnDefinition = "TEXT")
    private String senha;

    @Column(name = "dataNascimento", nullable = false)
    private Date dataNascimento;

    @Column(name = "idade", nullable = false)
    private Integer idade;

    @Column(name = "ativo")
    private Boolean ativo;

    @Column(name = "dataCadastro", nullable = false)
    private LocalDateTime dataCadastro;

    @Column(name = "dataUltimoAcesso", nullable = false)
    private LocalDateTime dataUltimoAcesso;

    // ADICIONADO: tipo de usuário para diferenciar CANDIDATO / EMPRESA / ADMIN
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_usuario", nullable = false, length = 20)
    private TipoUsuario tipoUsuario;

    // ── Consentimento LGPD (Art. 7º, I / Art. 8º) ────────────────────────────
    // Registro de quando e sob qual versão da política o titular consentiu.
    @Column(name = "consentimento_lgpd_em")
    private LocalDateTime consentimentoLgpdEm;

    @Column(name = "versao_politica_privacidade", length = 10)
    private String versaoPoliticaPrivacidade;

    // ── Verificação de e-mail ─────────────────────────────────────────────────
    // Contas criadas pelo cadastro público (/api/auth/register) só podem logar
    // após confirmar o e-mail. NULO = registro legado (anterior à feature) e é
    // tratado como verificado para não travar contas existentes.
    @Column(name = "email_verificado")
    private Boolean emailVerificado;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "telefones_usuario",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "telefone_id"))
    private List<Telefones> telefones;

    @PrePersist
    protected void onCreate() {
        this.dataCadastro = LocalDateTime.now();
        this.dataUltimoAcesso = LocalDateTime.now();
        if (this.ativo == null) {
            this.ativo = true;
        }
    }

    // Registra o consentimento do titular com a versão vigente da política (LGPD)
    public void registrarConsentimento() {
        this.consentimentoLgpdEm = LocalDateTime.now();
        this.versaoPoliticaPrivacidade = VERSAO_POLITICA_PRIVACIDADE_ATUAL;
    }

    // Enum interno para o tipo de usuário
    public enum TipoUsuario {
        CANDIDATO, EMPRESA, ADMIN
    }
}
