package com.matchvagas.backend.service.embedding;

import com.matchvagas.backend.entity.CandidatoEmbedding;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.VagaEmbedding;
import com.matchvagas.backend.entity.Vagas;
import com.matchvagas.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexacaoEmbeddingService {
    private final EmbeddingPort embeddingPort;
    private final VagaEmbeddingRepository vagaEmbeddingRepository;
    private final CandidatoEmbeddingRepository candidatoEmbeddingRepository;
    private final ExperienciaRepository experienciaRepository;
    private final FormacaoRepository formacaoRepository;

    public boolean isAtivo() { return embeddingPort.isAtivo(); }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean indexarVaga(Vagas vaga) {
        if (!embeddingPort.isAtivo() || vaga == null || vaga.getId() == null) return false;
        try {
            String texto = TextoEmbeddingBuilder.daVaga(vaga);
            String hash = sha256(texto);
            VagaEmbedding registro = vagaEmbeddingRepository.findByVagaId(vaga.getId())
                    .orElseGet(VagaEmbedding::new);
            if (hash.equals(registro.getTextoHash()) && embeddingPort.modelo().equals(registro.getModelo())) return false;
            float[] vetor = embeddingPort.embed(texto);
            registro.setVaga(vaga);
            registro.setVetor(EmbeddingCodec.toCsv(vetor));
            registro.setDim(vetor.length);
            registro.setModelo(embeddingPort.modelo());
            registro.setTextoHash(hash);
            vagaEmbeddingRepository.save(registro);
            return true;
        } catch (Exception e) {
            log.warn("Falha ao indexar embedding da vaga {}: {}", vaga.getId(), e.getMessage());
            return false;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean indexarCandidato(Candidatos candidato) {
        if (!embeddingPort.isAtivo() || candidato == null || candidato.getId() == null) return false;
        try {
            String texto = TextoEmbeddingBuilder.doCandidato(candidato,
                    experienciaRepository.findByCandidatoId(candidato.getId()),
                    formacaoRepository.findByCandidatoId(candidato.getId()));
            String hash = sha256(texto);
            CandidatoEmbedding registro = candidatoEmbeddingRepository.findByCandidatoId(candidato.getId())
                    .orElseGet(CandidatoEmbedding::new);
            if (hash.equals(registro.getTextoHash()) && embeddingPort.modelo().equals(registro.getModelo())) return false;
            float[] vetor = embeddingPort.embed(texto);
            registro.setCandidato(candidato);
            registro.setVetor(EmbeddingCodec.toCsv(vetor));
            registro.setDim(vetor.length);
            registro.setModelo(embeddingPort.modelo());
            registro.setTextoHash(hash);
            candidatoEmbeddingRepository.save(registro);
            return true;
        } catch (Exception e) {
            log.warn("Falha ao indexar embedding do candidato {}: {}", candidato.getId(), e.getMessage());
            return false;
        }
    }

    static String sha256(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
