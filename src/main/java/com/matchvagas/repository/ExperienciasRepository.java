package com.matchvagas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matchvagas.entity.Experiencia;

@Repository
public interface ExperienciasRepository extends JpaRepository<Experiencia, Long> {

    // Busca todas as experiências de um candidato pelo id
    List<Experiencia> findByCandidatoId(Long candidatoId);

    // Busca experiências por cargo
    List<Experiencia> findByCargo(String cargo);

    // Busca experiências por empresa (case-insensitive, parcial)
    List<Experiencia> findByEmpresaContainingIgnoreCase(String empresa);

    // Busca experiências por descrição (case-insensitive, parcial)
    List<Experiencia> findByDescricaoContainingIgnoreCase(String descricao);

    // Busca experiências por email do candidato (assumindo campo email em CandidatoVaga)
    List<Experiencia> findByCandidatoEmail(String email);

    // Busca uma experiência específica de um candidato
    Optional<Experiencia> findByIdAndCandidatoId(Long id, Long candidatoId);

}
