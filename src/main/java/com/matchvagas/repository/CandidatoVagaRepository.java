package com.matchvagas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matchvagas.entity.CandidatoVaga;

@Repository
public interface CandidatoVagaRepository extends JpaRepository<CandidatoVaga, Long> {

    Optional<CandidatoVaga> findByEmail(String email);

    List<CandidatoVaga> findByVaga(String vaga);

    List<CandidatoVaga> findByAtivo(Boolean ativo);

    List<CandidatoVaga> findByNomeContainingIgnoreCase(String nome);
}
