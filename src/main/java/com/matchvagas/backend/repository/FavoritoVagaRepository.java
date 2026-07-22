package com.matchvagas.backend.repository;

import com.matchvagas.backend.entity.FavoritoVaga;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FavoritoVagaRepository extends JpaRepository<FavoritoVaga, Long> {

    Page<FavoritoVaga> findByCandidatoIdOrderByDataFavoritadoDesc(Long candidatoId, Pageable pageable);

    boolean existsByCandidatoIdAndVagaId(Long candidatoId, Long vagaId);

    Optional<FavoritoVaga> findByCandidatoIdAndVagaId(Long candidatoId, Long vagaId);
}
