package com.matchvagas.backend.repository;

import com.matchvagas.backend.entity.VagaEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VagaEmbeddingRepository extends JpaRepository<VagaEmbedding, Long> {
    Optional<VagaEmbedding> findByVagaId(Long vagaId);
    List<VagaEmbedding> findByVagaIdIn(Collection<Long> vagaIds);
}
