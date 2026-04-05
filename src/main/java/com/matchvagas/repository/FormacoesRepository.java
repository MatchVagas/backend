package com.matchvagas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matchvagas.entity.Formacao;

@Repository
public interface FormacoesRepository extends JpaRepository<Formacao, Long> {

    @EntityGraph(attributePaths = {"candidato"})
    List<Formacao> findAll();

    @EntityGraph(attributePaths = {"candidato"})
    List<Formacao> findByCandidatoId(Long candidatoId);

    List<Formacao> findByNivel(String nivel);

    List<Formacao> findByCursoContainingIgnoreCase(String curso);

    List<Formacao> findByInstituicaoContainingIgnoreCase(String instituicao);

    List<Formacao> findByCandidatoEmail(String email);

    Optional<Formacao> findByIdAndCandidatoId(Long id, Long candidatoId);
}
