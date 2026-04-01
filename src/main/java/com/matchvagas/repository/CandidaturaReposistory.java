package com.matchvagas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matchvagas.backend.entity.Candidatura;

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


    @EntityGraph(attributePaths = {"candidato", "candidato.formacoes", "candidato.experiencias", "vaga"})
    List<Candidatura> findAll();

    @EntityGraph(attributePaths = {"candidato", "candidato.formacoes", "candidato.experiencias", "vaga"})
    Optional<Candidatura> findById(Long id);

    @EntityGraph(attributePaths = {"candidato", "candidato.formacoes", "candidato.experiencias", "vaga"})
    List<Candidatura> findByVagaId(Long vagaId);
}
