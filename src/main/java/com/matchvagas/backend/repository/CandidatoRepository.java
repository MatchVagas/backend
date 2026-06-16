package com.matchvagas.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matchvagas.backend.entity.Candidatos;

@Repository
public interface CandidatoRepository extends JpaRepository<Candidatos, Long> {

    // LGPD-04 — busca por unicidade via hash determinístico do CPF
    Optional<Candidatos> findByCpfHash(String cpfHash);
    Optional<Candidatos> findByUsuarioId(Long usuarioId);
 
}
