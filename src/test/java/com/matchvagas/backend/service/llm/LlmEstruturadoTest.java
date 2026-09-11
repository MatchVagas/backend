package com.matchvagas.backend.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchvagas.backend.exception.LlmIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fase 3 — chamada estruturada ao LLM")
class LlmEstruturadoTest {
    private final ObjectMapper mapper = new ObjectMapper();
    @Mock LlmPort port;

    private LlmEstruturado llm;

    @BeforeEach
    void setUp() {
        llm = new LlmEstruturado(port, mapper);
    }

    @Test
    void extraiJsonCercadoDeTextoECercasDeCodigo() {
        when(port.isAtivo()).thenReturn(true);
        when(port.gerarJson("s", "u")).thenReturn("Claro! ```json\n{\"descricao\": \"ok\"}\n```");

        assertThat(llm.gerar("s", "u", "descricao").get("descricao").asText()).isEqualTo("ok");
    }

    @Test
    void repeteUmaVezQuandoOJsonVemQuebrado() {
        when(port.isAtivo()).thenReturn(true);
        when(port.gerarJson(anyString(), anyString()))
                .thenReturn("{quebrado", "{\"parecer\": \"bom\", \"recomendacao\": \"AVANCAR\"}");

        JsonNode resposta = llm.gerar("s", "u", "parecer", "recomendacao");

        assertThat(resposta.get("parecer").asText()).isEqualTo("bom");
        verify(port, times(2)).gerarJson(anyString(), anyString());
    }

    @Test
    void campoObrigatorioAusenteContaComoRespostaInvalida() {
        when(port.isAtivo()).thenReturn(true);
        when(port.gerarJson(anyString(), anyString())).thenReturn("{\"descricao\": \"x\", \"requisitos\": \"  \"}");

        assertThatThrownBy(() -> llm.gerar("s", "u", "descricao", "requisitos"))
                .isInstanceOf(LlmIndisponivelException.class)
                .hasMessageContaining("resposta válida");
        verify(port, times(LlmEstruturado.TENTATIVAS)).gerarJson(anyString(), anyString());
    }

    @Test
    void desligadoFalhaSemChamarOModelo() {
        when(port.isAtivo()).thenReturn(false);

        assertThatThrownBy(() -> llm.gerar("s", "u"))
                .isInstanceOf(LlmIndisponivelException.class)
                .hasMessageContaining("não está habilitado");
        verify(port, never()).gerarJson(anyString(), anyString());
    }

    @Test
    void falhaDeRedeNaoRepete() {
        when(port.isAtivo()).thenReturn(true);
        when(port.gerarJson(anyString(), anyString())).thenThrow(new IllegalStateException("Connection refused"));

        assertThatThrownBy(() -> llm.gerar("s", "u"))
                .isInstanceOf(LlmIndisponivelException.class)
                .hasMessageContaining("indisponível");
        verify(port, times(1)).gerarJson(anyString(), anyString());
    }

    @Test
    void textoConverteListaEmItens() throws Exception {
        assertThat(LlmEstruturado.texto(mapper.readTree("[\"Java 21\", \"- Spring Boot\"]")))
                .isEqualTo("- Java 21\n- Spring Boot");
    }

    @Test
    void listaAceitaTextoComUmItemPorLinha() throws Exception {
        assertThat(LlmEstruturado.lista(mapper.readTree("\"- SQL\\n• Docker; Git\"")))
                .containsExactly("SQL", "Docker", "Git");
    }
}
