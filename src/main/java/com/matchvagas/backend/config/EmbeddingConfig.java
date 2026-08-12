package com.matchvagas.backend.config;

import com.matchvagas.backend.service.embedding.DisabledEmbeddingPort;
import com.matchvagas.backend.service.embedding.EmbeddingPort;
import com.matchvagas.backend.service.embedding.TransformersEmbeddingAdapter;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class EmbeddingConfig {
    @Bean
    @ConditionalOnProperty(name = "app.embeddings.enabled", havingValue = "true")
    EmbeddingModel transformersEmbeddingModel(
            @Value("${app.embeddings.model-uri:}") String modelUri,
            @Value("${app.embeddings.tokenizer-uri:}") String tokenizerUri,
            @Value("${app.embeddings.cache-dir}") String cacheDir) throws Exception {
        TransformersEmbeddingModel model = new TransformersEmbeddingModel();
        if (!modelUri.isBlank()) model.setModelResource(modelUri);
        if (!tokenizerUri.isBlank()) model.setTokenizerResource(tokenizerUri);
        model.setResourceCacheDirectory(cacheDir);
        model.setTokenizerOptions(Map.of("padding", "true", "truncation", "true"));
        model.afterPropertiesSet();
        return model;
    }

    @Bean
    @ConditionalOnProperty(name = "app.embeddings.enabled", havingValue = "true")
    EmbeddingPort transformersEmbeddingPort(
            EmbeddingModel model,
            @Value("${app.embeddings.dimensao}") int dimensao,
            @Value("${app.embeddings.modelo}") String nomeModelo) {
        return new TransformersEmbeddingAdapter(model, dimensao, nomeModelo);
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingPort.class)
    EmbeddingPort disabledEmbeddingPort() { return new DisabledEmbeddingPort(); }
}
