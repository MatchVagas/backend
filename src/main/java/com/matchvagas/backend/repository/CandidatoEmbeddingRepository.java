package com.matchvagas.backend.repository;

import com.matchvagas.backend.entity.CandidatoEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CandidatoEmbeddingRepository extends JpaRepository<CandidatoEmbedding, Long> {
    Optional<CandidatoEmbedding> findByCandidatoId(Long candidatoId);
}
