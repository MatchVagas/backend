package com.matchvagas.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LGPD-04 — Conversor JPA do CPF")
class CpfCryptoConverterTest {

    private final CpfCryptoConverter converter = new CpfCryptoConverter();

    @Test
    @DisplayName("grava cifrado e lê de volta em texto puro")
    void roundTrip() {
        String coluna = converter.convertToDatabaseColumn("529.982.247-25");

        assertThat(coluna).startsWith("enc:v1:").doesNotContain("529");
        assertThat(converter.convertToEntityAttribute(coluna)).isEqualTo("529.982.247-25");
    }

    @Test
    @DisplayName("nulo continua nulo nos dois sentidos")
    void nulo() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
