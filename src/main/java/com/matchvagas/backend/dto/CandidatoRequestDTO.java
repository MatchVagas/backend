package com.matchvagas.backend.dto;

import com.matchvagas.backend.entity.Genero;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import org.hibernate.validator.constraints.br.CPF;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CandidatoRequestDTO(

        @NotBlank(message = "CPF é obrigatório")
        @CPF(message = "CPF inválido")
        String cpf,

        String nomeCompleto,

        @Email(message = "Email inválido")
        String email,

        @Past(message = "Data de nascimento deve ser no passado")
        LocalDate dataNascimento,

        String resumoProfissional,

        String disponibilidade,

        @DecimalMin(value = "0.0", message = "Pretensão salarial não pode ser negativa")
        BigDecimal pretensaoSalarial,

        Genero genero,

        @Valid
        TelefonesRequestDTO telefone,

        // LGPD Art. 9º — finalidade do tratamento do endereço informada ao titular
        @Schema(description = "Endereço do candidato. Finalidade (LGPD Art. 9º): personalização "
                + "de vagas por localidade e exibição à empresa apenas quando o candidato autorizar "
                + "(compartilharEndereco). O endereço é pseudoanonimizado após longa inatividade "
                + "da conta (política de retenção).")
        @Valid
        LocalizacaoRequestDTO localizacao
) {}
