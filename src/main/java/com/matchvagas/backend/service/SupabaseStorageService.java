package com.matchvagas.backend.service;

import com.matchvagas.backend.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key}")
    private String serviceRoleKey;

    @Value("${supabase.storage.bucket:curriculos}")
    private String bucket;

    private RestClient client;

    @PostConstruct
    void init() {
        client = RestClient.builder()
                .baseUrl(supabaseUrl + "/storage/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey)
                .defaultHeader("apikey", serviceRoleKey)
                .build();
    }

    /**
     * Faz upload de um arquivo para o Supabase Storage.
     * Usa upsert (sobrescreve se já existir o mesmo objectPath).
     *
     * @param objectPath caminho dentro do bucket, ex: "42/abc123.pdf"
     * @param bytes      conteúdo do arquivo
     * @param contentType MIME type do arquivo
     */
    public void upload(String objectPath, byte[] bytes, String contentType) {
        try {
            client.put()
                    .uri("/object/" + bucket + "/" + objectPath)
                    .header("x-upsert", "true")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(bytes)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new BusinessException("Falha ao enviar arquivo para o storage: " + e.getMessage());
        }
    }

    /**
     * Gera uma URL assinada temporária para acesso privado ao arquivo.
     *
     * @param objectPath      caminho dentro do bucket
     * @param expiresInSeconds tempo de validade da URL em segundos
     * @return URL assinada completa (SUPABASE_URL + path assinado)
     */
    public String gerarUrlAssinada(String objectPath, int expiresInSeconds) {
        try {
            Map<?, ?> response = client.post()
                    .uri("/object/sign/" + bucket + "/" + objectPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("expiresIn", expiresInSeconds))
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("signedURL")) {
                throw new BusinessException("Resposta inválida ao gerar URL assinada.");
            }
            return supabaseUrl + response.get("signedURL");
        } catch (RestClientException e) {
            throw new BusinessException("Falha ao gerar URL de acesso: " + e.getMessage());
        }
    }

    /**
     * Remove um arquivo do Supabase Storage.
     *
     * @param objectPath caminho dentro do bucket
     */
    public void deletar(String objectPath) {
        try {
            client.method(HttpMethod.DELETE)
                    .uri("/object/" + bucket)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("prefixes", List.of(objectPath)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new BusinessException("Falha ao remover arquivo do storage: " + e.getMessage());
        }
    }
}
