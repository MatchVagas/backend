package com.matchvagas.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Exportação estruturada de todos os dados pessoais do candidato
 * (LGPD Art. 18, V — portabilidade / acesso facilitado aos dados).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MeusDadosExportDTO(
        LocalDateTime exportadoEm,
        Usuario usuario,
        Candidato candidato,
        List<Experiencia> experiencias,
        List<Formacao> formacoes,
        List<Candidatura> candidaturas
) {
    public record Usuario(
            String nome,
            String email,
            LocalDateTime dataCadastro,
            Integer idade,
            LocalDateTime consentimentoLgpdEm,
            String versaoPoliticaPrivacidade
    ) {}

    public record Candidato(
            String cpf,
            String objetivoProfissional,
            String disponibilidade,
            BigDecimal pretensaoSalarial,
            String genero,
            Endereco endereco,
            List<Habilidade> habilidades,
            List<Telefone> telefones
    ) {}

    public record Endereco(
            String logradouro,
            String numero,
            String complemento,
            String bairro,
            String cidade,
            String estado,
            String cep
    ) {}

    public record Habilidade(String nome, String nivel) {}

    public record Telefone(String numero, boolean whatsapp, String tipo) {}

    public record Experiencia(
            String empresa,
            String cargo,
            String descricao,
            String dataInicio,
            String dataFim
    ) {}

    public record Formacao(
            String instituicao,
            String curso,
            String nivel,
            String dataInicio,
            String dataFim
    ) {}

    public record Candidatura(
            String vaga,
            String empresa,
            String status,
            LocalDateTime dataCandidatura
    ) {}
}
