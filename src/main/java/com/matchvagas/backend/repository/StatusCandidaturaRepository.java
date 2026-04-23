package com.matchvagas.backend.repository;

import com.matchvagas.backend.entity.StatusCandidatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatusCandidaturaRepository extends JpaRepository<StatusCandidatura, Long> {}
