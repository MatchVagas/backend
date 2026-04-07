package com.matchvagas.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matchvagas.backend.entity.Candidatos;

@Repository
public interface CandidatoRepository extends JpaRepository<Candidatos, Long> {

    //@EntityGraph(attributePaths = {"cpf"})
    Optional<Candidatos> findByCpf(String cpf);
    Optional<Candidatos> findByUsuarioId(Long usuarioId);
 
}
