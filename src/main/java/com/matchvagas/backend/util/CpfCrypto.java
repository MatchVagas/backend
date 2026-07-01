package com.matchvagas.backend.util;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Criptografia e hash de CPF (LGPD-04, Art. 46).
 *
 * <ul>
 *   <li><b>Criptografia</b> (AES-256-GCM) protege o CPF em repouso, permitindo
 *       exibi-lo ao próprio titular (reversível com a chave).</li>
 *   <li><b>Hash</b> (HMAC-SHA256) é determinístico e serve para verificar
 *       unicidade sem expor o valor.</li>
 * </ul>
 *
 * As chaves vêm das variáveis de ambiente {@code CPF_ENCRYPTION_KEY} e
 * {@code CPF_HASH_SECRET}. Há um fallback de desenvolvimento (inseguro) para
 * ambientes locais/testes — <b>defina as variáveis em produção</b>.
 */
public final class CpfCrypto {

    private CpfCrypto() {}

    private static final String PREFIXO = "enc:v1:";
    private static final int    IV_TAMANHO = 12;   // 96 bits (recomendado para GCM)
    private static final int    TAG_BITS   = 128;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final SecretKeySpec AES_KEY  = derivarChaveAes();
    private static final byte[]        HMAC_KEY = obterSegredoHmac();

    // SEC: true quando qualquer uma das chaves caiu no fallback inseguro de
    // desenvolvimento (variável de ambiente ausente). O StartupSecretsValidator
    // usa isto para abortar a inicialização em produção.
    private static final boolean USANDO_FALLBACK =
            faltaSegredo("CPF_ENCRYPTION_KEY") || faltaSegredo("CPF_HASH_SECRET");

    /** Indica se o CPF está sendo protegido com chaves de desenvolvimento (inseguras). */
    public static boolean usandoChavesInseguras() {
        return USANDO_FALLBACK;
    }

    private static boolean faltaSegredo(String envVar) {
        String v = System.getenv(envVar);
        if (v == null || v.isBlank()) {
            v = System.getProperty(envVar);
        }
        return v == null || v.isBlank();
    }

    /** Cifra o CPF; retorna texto cifrado prefixado e em Base64. */
    public static String encrypt(String plain) {
        if (plain == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_TAMANHO];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, AES_KEY, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            byte[] combinado = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, combinado, 0, iv.length);
            System.arraycopy(cifrado, 0, combinado, iv.length, cifrado.length);

            return PREFIXO + Base64.getEncoder().encodeToString(combinado);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao cifrar CPF", e);
        }
    }

    /**
     * Decifra o CPF. Valores sem o prefixo são considerados legados (texto puro,
     * de antes da migração) e devolvidos como estão.
     */
    public static String decrypt(String stored) {
        if (stored == null) {
            return null;
        }
        if (!stored.startsWith(PREFIXO)) {
            return stored; // legado em texto puro
        }
        try {
            byte[] combinado = Base64.getDecoder().decode(stored.substring(PREFIXO.length()));
            byte[] iv = new byte[IV_TAMANHO];
            System.arraycopy(combinado, 0, iv, 0, IV_TAMANHO);
            byte[] cifrado = new byte[combinado.length - IV_TAMANHO];
            System.arraycopy(combinado, IV_TAMANHO, cifrado, 0, cifrado.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, AES_KEY, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao decifrar CPF", e);
        }
    }

    /** Hash HMAC-SHA256 determinístico do CPF (apenas dígitos), em hex. */
    public static String hash(String cpf) {
        if (cpf == null) {
            return null;
        }
        String digitos = cpf.replaceAll("[^0-9]", "");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(HMAC_KEY, "HmacSHA256"));
            byte[] h = mac.doFinal(digitos.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(h.length * 2);
            for (byte b : h) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar hash do CPF", e);
        }
    }

    private static SecretKeySpec derivarChaveAes() {
        String material = valor("CPF_ENCRYPTION_KEY",
                "dev-insecure-cpf-encryption-key-change-me");
        try {
            // SHA-256 garante 32 bytes (AES-256) independente do formato da entrada
            byte[] chave = MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(chave, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao derivar chave AES do CPF", e);
        }
    }

    private static byte[] obterSegredoHmac() {
        return valor("CPF_HASH_SECRET", "dev-insecure-cpf-hash-secret-change-me")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static String valor(String envVar, String fallback) {
        String v = System.getenv(envVar);
        if (v == null || v.isBlank()) {
            v = System.getProperty(envVar);
        }
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
