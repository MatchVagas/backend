package com.matchvagas.backend.config;

import com.matchvagas.backend.service.llm.DisabledLlmPort;
import com.matchvagas.backend.service.llm.LlmPort;
import com.matchvagas.backend.service.llm.OllamaLlmAdapter;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class LlmConfig {
    @Bean
    @ConditionalOnProperty(name = "app.llm.enabled", havingValue = "true")
    LlmPort ollamaLlmPort(
            @Value("${app.llm.base-url}") String baseUrl,
            @Value("${app.llm.modelo}") String modelo,
            @Value("${app.llm.temperatura}") double temperatura,
            @Value("${app.llm.num-ctx}") int numCtx,
            @Value("${app.llm.timeout-segundos}") int timeoutSegundos) {
        return criarOllama(baseUrl, modelo, temperatura, numCtx, timeoutSegundos);
    }

    @Bean
    @ConditionalOnMissingBean(LlmPort.class)
    LlmPort disabledLlmPort() { return new DisabledLlmPort(); }

    static LlmPort criarOllama(String baseUrl, String modelo, double temperatura, int numCtx, int timeoutSegundos) {
        // Geração em CPU pode levar minutos: o timeout de leitura precisa acomodar isso.
        SimpleClientHttpRequestFactory http = new SimpleClientHttpRequestFactory();
        http.setConnectTimeout(Duration.ofSeconds(5));
        http.setReadTimeout(Duration.ofSeconds(timeoutSegundos));
        OllamaApi api = OllamaApi.builder()
                .baseUrl(baseUrl)
                .restClientBuilder(RestClient.builder().requestFactory(http))
                .build();
        OllamaChatModel chat = OllamaChatModel.builder()
                .ollamaApi(api)
                .defaultOptions(OllamaChatOptions.builder()
                        .model(modelo)
                        .temperature(temperatura)
                        .numCtx(numCtx)
                        .format("json")
                        .build())
                // Sem retry automático: cada tentativa pode custar minutos de CPU.
                // LlmEstruturado já repete uma vez quando o JSON vem inválido.
                .retryTemplate(RetryTemplate.builder().maxAttempts(1).build())
                .build();
        return new OllamaLlmAdapter(chat, modelo);
    }
}