package com.matchvagas.repository;

import java.util.List;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matchvagas.entity.Candidatura;

@Repository
public interface CandidaturaRepository extends JpaRepository<Candidatura, Long> {

    // Busca por status da candidatura
    List<Candidatura> findByStatus(String status);

    // Busca por id do candidato
    List<Candidatura> findByCandidatoId(Long candidatoId);

    // Busca por id da vaga
    List<Candidatura> findByVagaId(Long vagaId);

    // Busca por email do candidato (assumindo que CandidatoVaga tem campo email)
    List<Candidatura> findByCandidatoEmail(String email);

}
