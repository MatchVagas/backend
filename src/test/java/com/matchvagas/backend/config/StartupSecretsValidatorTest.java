package com.matchvagas.backend.config;

import com.matchvagas.backend.util.CpfCrypto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;

@DisplayName("SEC-05 — validação dos segredos na inicialização")
class StartupSecretsValidatorTest {

    private static final String JWT_FORTE = "Zx8q2Lr9Vb4Tn6Wm1Ks3Pd7Yh5Fj0Gc8Qe2Ua4Ri6So9";

    private StartupSecretsValidator validator(String appEnv, String jwt, String... profiles) {
        MockEnvironment env = new MockEnvironment().withProperty("app.env", appEnv);
        env.setActiveProfiles(profiles);
        return new StartupSecretsValidator(env, jwt);
    }

    @Test
    @DisplayName("em dev, segredos fracos só geram aviso")
    void devApenasAvisa() {
        assertThatCode(() -> validator("dev", "curto").validar()).doesNotThrowAnyException();
        assertThatCode(() -> validator("dev", "").validar()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("em prod, JWT ausente aborta a inicialização")
    void prodSemJwt() {
        assertThatThrownBy(() -> validator("prod", "").validar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET não definido");
    }

    @Test
    @DisplayName("em prod, JWT curto ou placeholder aborta a inicialização")
    void prodJwtFraco() {
        assertThatThrownBy(() -> validator("production", "curto").validar())
                .hasMessageContaining("muito curto");
        assertThatThrownBy(() -> validator("prod", "changeme-changeme-changeme-changeme-123").validar())
                .hasMessageContaining("placeholder");
    }

    @Test
    @DisplayName("o profile 'prod' também ativa o modo estrito")
    void profileProd() {
        assertThatThrownBy(() -> validator("dev", "", "prod").validar())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("em prod, sem as chaves de CPF aborta mesmo com JWT forte")
    void prodSemChavesCpf() {
        assumeThat(CpfCrypto.usandoChavesInseguras()).isTrue();

        assertThatThrownBy(() -> validator("prod", JWT_FORTE).validar())
                .hasMessageContaining("CPF_ENCRYPTION_KEY");
    }
}
