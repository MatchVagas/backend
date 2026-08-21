package com.matchvagas.backend.config;

import com.matchvagas.backend.util.CpfCrypto;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Valida, na inicialização, que os segredos críticos de segurança estão
 * configurados de forma forte (SEC-05 / LGPD Art. 46).
 *
 * <p>Em produção ({@code APP_ENV=prod} ou profile {@code prod} ativo) a aplicação
 * <b>se recusa a subir</b> se algum segredo estiver ausente, fraco ou usando o
 * valor placeholder — evitando o cenário em que um deploy "funciona" mas com JWT
 * forjável ou CPF cifrado com chave pública conhecida.
 *
 * <p>Em desenvolvimento apenas registra um aviso, para não atrapalhar testes
 * locais.
 */
@Slf4j
@Component
public class StartupSecretsValidator {

    private final Environment environment;
    private final String jwtSecret;

    public StartupSecretsValidator(Environment environment,
                                   @Value("${jwt.secret:}") String jwtSecret) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
    }

    @PostConstruct
    public void validar() {
        List<String> problemas = new ArrayList<>();

        // ── JWT ────────────────────────────────────────────────────────────────
        if (jwtSecret == null || jwtSecret.isBlank()) {
            problemas.add("JWT_SECRET não definido.");
        } else {
            if (jwtSecret.getBytes().length < 32) {
                problemas.add("JWT_SECRET muito curto (mínimo 32 bytes / 256 bits para HS256).");
            }
            String lower = jwtSecret.toLowerCase();
            if (lower.contains("troque_em_producao") || lower.contains("change") || lower.contains("changeme")) {
                problemas.add("JWT_SECRET ainda usa um valor placeholder — gere um com 'openssl rand -base64 64'.");
            }
        }

        // ── CPF (AES-256-GCM + HMAC-SHA256) ─────────────────────────────────────
        if (CpfCrypto.usandoChavesInseguras()) {
            problemas.add("CPF_ENCRYPTION_KEY / CPF_HASH_SECRET ausentes — usando fallback inseguro de desenvolvimento.");
        }

        if (problemas.isEmpty()) {
            return;
        }

        String msg = "Segredos de segurança mal configurados:\n  - " + String.join("\n  - ", problemas);

        if (isProducao()) {
            throw new IllegalStateException(
                    "ABORTANDO INICIALIZAÇÃO — " + msg
                  + "\nDefina os segredos via variáveis de ambiente antes de subir em produção.");
        }

        log.warn("⚠️  [DEV] {}\n  (Com APP_ENV=prod isto impediria a inicialização.)", msg);
    }

    private boolean isProducao() {
        String appEnv = environment.getProperty("app.env", "dev");
        if ("prod".equalsIgnoreCase(appEnv) || "production".equalsIgnoreCase(appEnv)) {
            return true;
        }
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }
}
