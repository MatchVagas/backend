package com.matchvagas.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SEC-05 — Validação de arquivo pela assinatura (magic bytes)")
class FileSignatureValidatorTest {

    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private static byte[] bytes(int... valores) {
        byte[] b = new byte[valores.length];
        for (int i = 0; i < valores.length; i++) b[i] = (byte) valores[i];
        return b;
    }

    static Stream<Arguments> assinaturasValidas() {
        return Stream.of(
                Arguments.of("application/pdf", bytes(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31)),
                Arguments.of("image/jpeg", bytes(0xFF, 0xD8, 0xFF, 0xE0)),
                Arguments.of("image/png", bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00)),
                Arguments.of("image/webp", bytes(0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50)),
                Arguments.of("application/msword", bytes(0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1)),
                Arguments.of(DOCX, bytes(0x50, 0x4B, 0x03, 0x04, 0x14)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("assinaturasValidas")
    @DisplayName("aceita a assinatura correta de cada tipo")
    void aceitaAssinaturaCorreta(String mime, byte[] header) {
        assertThat(FileSignatureValidator.matches(header, mime)).isTrue();
    }

    @Test
    @DisplayName("rejeita executável declarado como PDF")
    void rejeitaConteudoForjado() {
        assertThat(FileSignatureValidator.matches(bytes(0x4D, 0x5A, 0x90, 0x00), "application/pdf")).isFalse();
    }

    @Test
    @DisplayName("rejeita RIFF que não é WebP (ex.: WAV)")
    void rejeitaRiffNaoWebp() {
        byte[] wav = bytes(0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x41, 0x56, 0x45);
        assertThat(FileSignatureValidator.matches(wav, "image/webp")).isFalse();
        assertThat(FileSignatureValidator.matches(bytes(0x52, 0x49, 0x46, 0x46), "image/webp")).isFalse();
    }

    @Test
    @DisplayName("rejeita cabeçalho curto, nulo e tipo não suportado")
    void rejeitaEntradasInvalidas() {
        assertThat(FileSignatureValidator.matches(bytes(0x89, 0x50), "image/png")).isFalse();
        assertThat(FileSignatureValidator.matches(null, "application/pdf")).isFalse();
        assertThat(FileSignatureValidator.matches(bytes(0x25, 0x50, 0x44, 0x46), null)).isFalse();
        assertThat(FileSignatureValidator.matches(bytes(0x47, 0x49, 0x46, 0x38), "image/gif")).isFalse();
    }
}
