package com.matchvagas.backend.service.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Fase 3 — minimização de dados pessoais antes do LLM")
class MascaraDadosPessoaisTest {

    @Test
    void mascaraContatosCpfEPerfis() {
        String texto = "Maria — maria.silva@email.com — (11) 98765-4321 — +55 21 3456-7890 — "
                + "CPF 123.456.789-09 — linkedin.com/in/maria";

        assertThat(MascaraDadosPessoais.mascarar(texto))
                .doesNotContain("maria.silva@", "98765", "3456-7890", "123.456", "linkedin.com/in")
                .contains("[e-mail omitido]", "[telefone omitido]", "[CPF omitido]", "[perfil omitido]");
    }

    @Test
    void preservaPeriodosEValoresQueParecemTelefone() {
        String texto = "Analista (2019-2021), Dev 2021 - 2024, salário R$ 5.000,00, 40 horas";

        assertThat(MascaraDadosPessoais.mascarar(texto)).isEqualTo(texto);
    }
}
