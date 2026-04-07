package com.matchvagas.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matchvagas.backend.entity.Curriculos;

@Repository
public interface CurriculoRepository extends JpaRepository<Curriculos, Long> {
    Optional<Curriculos> findByCandidatoId(Long candidatoId);
    
}
