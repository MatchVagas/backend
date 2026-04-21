package com.matchvagas.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matchvagas.backend.entity.StatusVaga;

import java.util.Optional;

@Repository
public interface StatusVagaRepository extends JpaRepository<StatusVaga, Integer> {
    Optional<StatusVaga> findByDescricao(String descricao);
} 