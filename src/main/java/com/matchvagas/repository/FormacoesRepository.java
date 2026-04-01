package com.matchvagas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matchvagas.backend.entity.Formacoes;

@Repository
public interface FormacoesRepository extends JpaRepository<Formacoes, Long> {

    @EntityGraph(attributePaths = {"candidato"})
    List<Formacoes> findAll();

    @EntityGraph(attributePaths = {"candidato"})
    List<Formacoes> findByCandidatoId(Long candidatoId);

    List<Formacoes> findByNivel(String nivel);

    List<Formacoes> findByCursoContainingIgnoreCase(String curso);

    List<Formacoes> findByInstituicaoContainingIgnoreCase(String instituicao);

    List<Formacoes> findByCandidatoEmail(String email);

    Optional<Formacoes> findByIdAndCandidatoId(Long id, Long candidatoId);
}
