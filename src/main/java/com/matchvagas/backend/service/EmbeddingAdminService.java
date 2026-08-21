package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.EmbeddingBackfillResponseDTO;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.VagaRepository;
import com.matchvagas.backend.service.embedding.IndexacaoEmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmbeddingAdminService {
    private final VagaRepository vagaRepository;
    private final CandidatoRepository candidatoRepository;
    private final IndexacaoEmbeddingService indexacao;

    public EmbeddingBackfillResponseDTO backfill() {
        if (!indexacao.isAtivo()) return new EmbeddingBackfillResponseDTO(false, 0, 0, 0);
        var vagas = vagaRepository.findAll();
        var candidatos = candidatoRepository.findAll();
        int atualizados = 0;
        for (var vaga : vagas) if (indexacao.indexarVaga(vaga)) atualizados++;
        for (var candidato : candidatos) if (indexacao.indexarCandidato(candidato)) atualizados++;
        return new EmbeddingBackfillResponseDTO(true, vagas.size(), candidatos.size(), atualizados);
    }
}
