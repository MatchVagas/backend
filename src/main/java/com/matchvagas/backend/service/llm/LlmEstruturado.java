package com.matchvagas.backend.service.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchvagas.backend.exception.LlmIndisponivelException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Chama o {@link LlmPort} esperando um objeto JSON. Modelos locais pequenos às vezes
 * devolvem JSON quebrado, cercado de texto ou com o tipo trocado (lista no lugar de
 * texto): a resposta é extraída e validada e, se inválida, pedida mais uma vez.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmEstruturado {
    static final int TENTATIVAS = 2;

    private final LlmPort llmPort;
    private final ObjectMapper objectMapper;

    public String modelo() { return llmPort.modelo(); }

    /** Falha rápido (503) — antes de consumir cota ou montar prompt — quando o LLM está desligado. */
    public void exigirAtivo() {
        if (!llmPort.isAtivo()) {
            throw new LlmIndisponivelException("O assistente de IA não está habilitado neste ambiente.");
        }
    }

    public JsonNode gerar(String sistema, String usuario, String... camposObrigatorios) {
        exigirAtivo();
        for (int tentativa = 1; tentativa <= TENTATIVAS; tentativa++) {
            String bruto;
            try {
                bruto = llmPort.gerarJson(sistema, usuario);
            } catch (RuntimeException e) {
                // Rede, timeout ou modelo ausente: repetir na hora não ajuda.
                log.warn("Falha ao chamar o LLM {}: {}", llmPort.modelo(), e.getMessage());
                throw new LlmIndisponivelException(
                        "O assistente de IA está indisponível no momento. Tente novamente em instantes.", e);
            }
            JsonNode resposta = interpretar(bruto);
            List<String> ausentes = resposta == null ? List.of() : Arrays.stream(camposObrigatorios)
                    .filter(campo -> texto(resposta.get(campo)) == null)
                    .toList();
            if (resposta != null && ausentes.isEmpty()) return resposta;
            log.warn("Resposta inválida do LLM {} (tentativa {}/{}): {}", llmPort.modelo(), tentativa,
                    TENTATIVAS, resposta == null ? "JSON ilegível" : "campos ausentes " + ausentes);
        }
        throw new LlmIndisponivelException(
                "O assistente de IA não conseguiu gerar uma resposta válida. Tente novamente.");
    }

    private JsonNode interpretar(String bruto) {
        if (bruto == null) return null;
        int inicio = bruto.indexOf('{');
        int fim = bruto.lastIndexOf('}');
        if (inicio < 0 || fim <= inicio) return null;
        try {
            JsonNode no = objectMapper.readTree(bruto.substring(inicio, fim + 1));
            return no != null && no.isObject() ? no : null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /** Texto de um campo; uma lista vira itens iniciados por "- ". Vazio vira null. */
    public static String texto(JsonNode no) {
        if (no == null || no.isNull() || no.isMissingNode()) return null;
        if (no.isArray()) {
            List<String> itens = lista(no);
            return itens.isEmpty() ? null : String.join("\n", itens.stream().map(i -> "- " + i).toList());
        }
        String s = no.isValueNode() ? no.asText().strip() : no.toString();
        return s.isEmpty() ? null : s;
    }

    /** Lista de um campo; aceita array ou texto com um item por linha ou ';'. */
    public static List<String> lista(JsonNode no) {
        if (no == null || no.isNull() || no.isMissingNode()) return List.of();
        List<String> itens = new ArrayList<>();
        if (no.isArray()) {
            no.forEach(e -> adicionar(itens, e.isValueNode() ? e.asText() : e.toString()));
        } else if (no.isValueNode()) {
            for (String parte : no.asText().split("\\R|;")) adicionar(itens, parte);
        }
        return itens;
    }

    private static void adicionar(List<String> itens, String bruto) {
        if (bruto == null) return;
        String s = bruto.strip().replaceFirst("^[-•*]\\s*", "").replaceAll("\\s+", " ").strip();
        if (!s.isEmpty()) itens.add(s);
    }
}