package com.matchvagas.backend.repository;

import com.matchvagas.backend.entity.StatusVaga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StatusVagaRepository extends JpaRepository<StatusVaga, Long> {
    Optional<StatusVaga> findByDescricao(String descricao);
}
