package com.matchvagas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matchvagas.entity.Candidaturas;

@Repository
public interface CandidaturaRepository extends JpaRepository<Candidaturas, Long> {

    // Busca por status da candidatura
    List<Candidaturas> findByStatus(String status);

    // Busca por id do candidato
    List<Candidaturas> findByCandidatoId(Long candidatoId);

    // Busca por id da vaga
    List<Candidaturas> findByVagaId(Long vagaId);

    // Busca por email do candidato (assumindo que CandidatoVaga tem campo email)
    List<Candidaturas> findByCandidatoEmail(String email);


    @EntityGraph(attributePaths = {"candidato", "candidato.formacoes", "candidato.experiencias", "vaga"})
    List<Candidaturas> findAll();

    @EntityGraph(attributePaths = {"candidato", "candidato.formacoes", "candidato.experiencias", "vaga"})
    Optional<Candidaturas> findById(Long id);

    @EntityGraph(attributePaths = {"candidato", "candidato.formacoes", "candidato.experiencias", "vaga"})
    List<Candidaturas> findByVagaId(Long vagaId);
}
