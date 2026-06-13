package com.matchvagas.backend.repository;

import com.matchvagas.backend.entity.HistoricoStatusCandidatura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoStatusCandidaturaRepository
        extends JpaRepository<HistoricoStatusCandidatura, Long> {

    List<HistoricoStatusCandidatura> findByCandidaturaIdOrderByDataHoraDesc(Long candidaturaId);
}
