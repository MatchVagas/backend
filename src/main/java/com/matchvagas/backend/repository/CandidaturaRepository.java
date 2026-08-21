package com.matchvagas.backend.repository;

import java.util.List;
import java.util.Optional;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matchvagas.backend.entity.Candidatura;
import com.matchvagas.backend.entity.StatusCandidatura;
import com.matchvagas.backend.entity.Vagas;

@Repository
public interface CandidaturaRepository extends JpaRepository<Candidatura, Long> {

    /*
    // Busca por status da candidatura
    List<Candidatura> findByStatus(StatusCandidatura status);

    // Busca por id do candidato
    List<Candidatura> findByCandidatoId(Long candidatoId);

    // Busca por id da vaga
    List<Candidatura> findByVagaId(Vagas vaga);

    // Busca por email do candidato (assumindo que CandidatoVaga tem campo email)
    //List<Candidatura> findByCandidatoEmail(String email);*/

    List<Candidatura> findByCandidatoId(Long candidatoId);

    // Overloads paginados (listagens grandes: minhas candidaturas / recebidas pela empresa)
    Page<Candidatura> findByCandidatoId(Long candidatoId, Pageable pageable);

    List<Candidatura> findByVagaId(Long vagaId);

    Optional<Candidatura> findByCandidatoIdAndVagaId(Long candidatoId, Long vagaId);

    boolean existsByCandidatoIdAndVagaId(Long candidatoId, Long vagaId);

    void deleteByVagaId(Long vagaId);

    // Busca todas as candidaturas das vagas de uma empresa
    List<Candidatura> findByVagaEmpresasId(Long empresaId);

    Page<Candidatura> findByVagaEmpresasId(Long empresaId, Pageable pageable);
}
