package com.matchvagas.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matchvagas.backend.entity.Modalidade;

@Repository
public interface ModalidadeRepository extends JpaRepository<Modalidade, Integer> {
    
}
