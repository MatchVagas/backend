package com.matchvagas.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CurriculoParserServiceTest {
    private final CurriculoParserService service = new CurriculoParserService(new ObjectMapper());

    @Test
    void extraiTextoEInformacoesEstruturadas() {
        String curriculo = """
                Maria da Silva
                maria.silva@email.com | (93) 99123-4567

                Resumo
                Desenvolvedora Java com Spring Boot, PostgreSQL, Docker e Git.

                Experiência profissional
                Desenvolvedora Backend - Empresa XPTO
                APIs REST e microserviços

                Formação acadêmica
                Sistemas de Informação - Universidade Exemplo
                """;

        var resultado = service.parse(curriculo.getBytes(StandardCharsets.UTF_8));

        assertThat(resultado.nome()).isEqualTo("Maria da Silva");
        assertThat(resultado.email()).isEqualTo("maria.silva@email.com");
        assertThat(resultado.telefone()).contains("99123-4567");
        assertThat(resultado.competencias()).contains("java", "spring boot", "postgresql", "docker", "git", "rest");
        assertThat(service.toJson(resultado)).contains("maria.silva@email.com", "competencias");
    }
}
