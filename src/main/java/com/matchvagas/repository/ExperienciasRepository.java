package com.matchvagas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matchvagas.entity.Experiencias;

@Repository
public interface ExperienciasRepository extends JpaRepository<Experiencias, Long> {

    // Busca todas as experiências de um candidato pelo id
    List<Experiencias> findByCandidatoId(Long candidatoId);

    // Busca experiências por cargo
    List<Experiencias> findByCargo(String cargo);

    // Busca experiências por empresa (case-insensitive, parcial)
    List<Experiencias> findByEmpresaContainingIgnoreCase(String empresa);

    // Busca experiências por descrição (case-insensitive, parcial)
    List<Experiencias> findByDescricaoContainingIgnoreCase(String descricao);

    // Busca experiências por email do candidato (assumindo campo email em CandidatoVaga)
    List<Experiencias> findByCandidatoEmail(String email);

    // Busca uma experiência específica de um candidato
    Optional<Experiencias> findByIdAndCandidatoId(Long id, Long candidatoId);


    @EntityGraph(attributePaths = {"candidato"})
    List<Experiencias> findAll();

    @EntityGraph(attributePaths = {"candidato"})
    Optional<Experiencias> findById(Long id);

    @EntityGraph(attributePaths = {"candidato"})
    List<Experiencias> findByCandidatoId(Long candidatoId);
}
