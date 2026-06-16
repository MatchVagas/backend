package com.matchvagas.backend.util;

/**
 * Verifica o conteúdo real de um arquivo pelos seus "magic bytes" (assinatura),
 * em vez de confiar no Content-Type enviado pelo cliente — que é facilmente
 * forjável (SEC-05).
 */
public final class FileSignatureValidator {

    private FileSignatureValidator() {}

    /**
     * Confere se o cabeçalho do arquivo corresponde ao MIME type esperado.
     *
     * @param header     primeiros bytes do arquivo (recomenda-se >= 12 bytes)
     * @param mimeType   MIME type declarado/esperado
     * @return true se a assinatura bate com o tipo esperado
     */
    public static boolean matches(byte[] header, String mimeType) {
        if (header == null || mimeType == null) {
            return false;
        }
        return switch (mimeType) {
            // %PDF
            case "application/pdf" -> startsWith(header, 0x25, 0x50, 0x44, 0x46);
            // JPEG: FF D8 FF
            case "image/jpeg" -> startsWith(header, 0xFF, 0xD8, 0xFF);
            // PNG: 89 50 4E 47 0D 0A 1A 0A
            case "image/png" -> startsWith(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            // WebP: "RIFF"...."WEBP"
            case "image/webp" -> startsWith(header, 0x52, 0x49, 0x46, 0x46)
                    && header.length >= 12
                    && header[8] == (byte) 0x57 && header[9] == (byte) 0x45
                    && header[10] == (byte) 0x42 && header[11] == (byte) 0x50;
            // DOC (OLE2 Compound File): D0 CF 11 E0 A1 B1 1A E1
            case "application/msword" ->
                    startsWith(header, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1);
            // DOCX (ZIP container): 50 4B 03 04
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                    startsWith(header, 0x50, 0x4B, 0x03, 0x04);
            default -> false;
        };
    }

    private static boolean startsWith(byte[] header, int... signature) {
        if (header.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (header[i] != (byte) signature[i]) {
                return false;
            }
        }
        return true;
    }
}
