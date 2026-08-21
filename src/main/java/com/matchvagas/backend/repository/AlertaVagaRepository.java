package com.matchvagas.backend.repository;

import com.matchvagas.backend.entity.AlertaVaga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertaVagaRepository extends JpaRepository<AlertaVaga, Long> {

    List<AlertaVaga> findByCandidatoIdOrderByDataCriacaoDesc(Long candidatoId);

    List<AlertaVaga> findByAtivoTrue();

    Optional<AlertaVaga> findByIdAndCandidatoId(Long id, Long candidatoId);
}
