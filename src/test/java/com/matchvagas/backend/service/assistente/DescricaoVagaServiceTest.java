package com.matchvagas.backend.service.assistente;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchvagas.backend.dto.DescricaoVagaRequestDTO;
import com.matchvagas.backend.exception.RateLimitException;
import com.matchvagas.backend.service.RateLimiterService;
import com.matchvagas.backend.service.llm.LlmEstruturado;
import com.matchvagas.backend.service.llm.LlmPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fase 3 — rascunho de descrição de vaga")
class DescricaoVagaServiceTest {
    private static final DescricaoVagaRequestDTO FORMULARIO = new DescricaoVagaRequestDTO(
            "Desenvolvedor Backend", "Tecnologia", null, "Remoto", null, null, null,
            new BigDecimal("8000"), new BigDecimal("12000"), null, "Stack Java 21 e PostgreSQL");

    @Mock LlmPort port;

    @Test
    void usaSoOsCamposInformadosEConverteListaEmItens() {
        DescricaoVagaService service = servico(30);
        when(port.isAtivo()).thenReturn(true);
        when(port.modelo()).thenReturn("qwen2.5:7b");
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        when(port.gerarJson(eq(PromptsAssistivos.SISTEMA_DESCRICAO), prompt.capture()))
                .thenReturn("{\"descricao\": \"Vaga para backend.\", \"requisitos\": [\"Java\", \"SQL\"]}");

        var rascunho = service.gerar(7L, FORMULARIO);

        assertThat(rascunho.descricao()).isEqualTo("Vaga para backend.");
        assertThat(rascunho.requisitos()).isEqualTo("- Java\n- SQL");
        assertThat(prompt.getValue())
                .contains("Título: Desenvolvedor Backend", "Modalidade: Remoto", "Stack Java 21 e PostgreSQL")
                .doesNotContain("Benefícios", "Tipo de contratação");
    }

    @Test
    void cotaPorUsuarioBarraExcessoAntesDeChamarOModelo() {
        DescricaoVagaService service = servico(1);
        when(port.isAtivo()).thenReturn(true);
        when(port.modelo()).thenReturn("qwen2.5:7b");
        when(port.gerarJson(anyString(), anyString()))
                .thenReturn("{\"descricao\": \"a\", \"requisitos\": \"b\"}");

        service.gerar(7L, FORMULARIO);

        assertThatThrownBy(() -> service.gerar(7L, FORMULARIO)).isInstanceOf(RateLimitException.class);
        verify(port, times(1)).gerarJson(anyString(), anyString());
    }

    private DescricaoVagaService servico(int limitePorHora) {
        LlmEstruturado llm = new LlmEstruturado(port, new ObjectMapper());
        return new DescricaoVagaService(llm, new CotaAssistente(llm, new RateLimiterService(), limitePorHora));
    }
}
