package com.matchvagas.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LGPD-04 — Criptografia e hash de CPF")
class CpfCryptoTest {

    @Test
    @DisplayName("encrypt/decrypt deve fazer round-trip do CPF")
    void roundTrip() {
        String cpf = "529.982.247-25";
        String cifrado = CpfCrypto.encrypt(cpf);

        assertThat(cifrado).isNotNull().startsWith("enc:v1:").isNotEqualTo(cpf);
        assertThat(CpfCrypto.decrypt(cifrado)).isEqualTo(cpf);
    }

    @Test
    @DisplayName("encrypt deve ser não-determinístico (IV aleatório)")
    void naoDeterministico() {
        String cpf = "52998224725";
        assertThat(CpfCrypto.encrypt(cpf)).isNotEqualTo(CpfCrypto.encrypt(cpf));
    }

    @Test
    @DisplayName("hash deve ser determinístico e ignorar a máscara")
    void hashDeterministico() {
        String comMascara = "529.982.247-25";
        String semMascara = "52998224725";

        assertThat(CpfCrypto.hash(comMascara))
                .isEqualTo(CpfCrypto.hash(semMascara))
                .hasSize(64); // SHA-256 em hex
    }

    @Test
    @DisplayName("decrypt deve tratar valores legados em texto puro")
    void legadoTextoPuro() {
        assertThat(CpfCrypto.decrypt("52998224725")).isEqualTo("52998224725");
    }

    @Test
    @DisplayName("null deve ser tratado com segurança")
    void valoresNulos() {
        assertThat(CpfCrypto.encrypt(null)).isNull();
        assertThat(CpfCrypto.decrypt(null)).isNull();
        assertThat(CpfCrypto.hash(null)).isNull();
    }
}
