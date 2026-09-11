package com.matchvagas.backend.repository;

import com.matchvagas.backend.entity.CandidatoEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface CandidatoEmbeddingRepository extends JpaRepository<CandidatoEmbedding, Long> {
    Optional<CandidatoEmbedding> findByCandidatoId(Long candidatoId);
    List<CandidatoEmbedding> findByCandidatoIdIn(Collection<Long> candidatoIds);
}
