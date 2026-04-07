package com.matchvagas.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matchvagas.backend.entity.Experiencia;

@Repository
public interface ExperienciaRepository extends JpaRepository<Experiencia, Long> {
    List<Experiencia> findByCandidatoId(Long candidatoId);
}
