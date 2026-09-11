package com.matchvagas.backend.service.assistente;

import com.fasterxml.jackson.databind.JsonNode;
import com.matchvagas.backend.dto.DescricaoVagaRequestDTO;
import com.matchvagas.backend.dto.DescricaoVagaResponseDTO;
import com.matchvagas.backend.service.llm.LlmEstruturado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** Rascunha descrição e requisitos a partir do formulário da vaga. Stateless: nada é salvo. */
@Service
@RequiredArgsConstructor
public class DescricaoVagaService {
    private final LlmEstruturado llm;
    private final CotaAssistente cota;

    public DescricaoVagaResponseDTO gerar(Long usuarioId, DescricaoVagaRequestDTO dto) {
        cota.consumir(usuarioId);
        JsonNode resposta = llm.gerar(PromptsAssistivos.SISTEMA_DESCRICAO,
                PromptsAssistivos.usuarioDescricao(dto), "descricao", "requisitos");
        return new DescricaoVagaResponseDTO(
                LlmEstruturado.texto(resposta.get("descricao")),
                LlmEstruturado.texto(resposta.get("requisitos")),
                llm.modelo(),
                LocalDateTime.now());
    }
}
