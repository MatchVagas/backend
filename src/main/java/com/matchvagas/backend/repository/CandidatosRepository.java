package com.matchvagas.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matchvagas.backend.entity.Candidatos;

@Repository
public interface CandidatosRepository extends JpaRepository<Candidatos, Long> {

    //@EntityGraph(attributePaths = {"cpf"})
    Optional<Candidatos> findByCpf(String cpf);

    //List<Candidatos> findByVaga(Vagas vaga);

    //List<Candidatos> findByAtivo(Boolean ativo);

    //List<Candidatos> findByNomeContainingIgnoreCase(String nome);
}
