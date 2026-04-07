package com.matchvagas.backend.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface EscolaridadeRepository extends JpaRepository<Escolaridades, Long> {
    
}
