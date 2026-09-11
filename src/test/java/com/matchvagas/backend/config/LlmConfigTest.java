package com.matchvagas.backend.config;

import com.matchvagas.backend.service.llm.LlmPort;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercita o adaptador Ollama de verdade contra um servidor HTTP falso — sem Ollama instalado. */
@DisplayName("Fase 3 — adaptador Ollama")
class LlmConfigTest {

    @Test
    void enviaFormatoJsonEDevolveOConteudoDaResposta() throws Exception {
        AtomicReference<String> requisicao = new AtomicReference<>();
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/chat", troca -> {
            requisicao.set(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] corpo = """
                    {"model": "qwen2.5:7b", "created_at": "2026-01-01T00:00:00Z",
                     "message": {"role": "assistant", "content": "{\\"ok\\": true}"},
                     "done": true, "done_reason": "stop", "total_duration": 1, "load_duration": 1,
                     "prompt_eval_count": 1, "prompt_eval_duration": 1, "eval_count": 1, "eval_duration": 1}
                    """.getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().add("Content-Type", "application/json");
            troca.sendResponseHeaders(200, corpo.length);
            troca.getResponseBody().write(corpo);
            troca.close();
        });
        servidor.start();
        try {
            LlmPort port = LlmConfig.criarOllama(
                    "http://127.0.0.1:" + servidor.getAddress().getPort(), "qwen2.5:7b", 0.2, 8192, 10);

            assertThat(port.gerarJson("instrução do sistema", "dados do usuário")).isEqualTo("{\"ok\": true}");
            assertThat(requisicao.get())
                    .contains("\"format\":\"json\"", "\"model\":\"qwen2.5:7b\"",
                            "instrução do sistema", "dados do usuário");
        } finally {
            servidor.stop(0);
        }
    }
}
