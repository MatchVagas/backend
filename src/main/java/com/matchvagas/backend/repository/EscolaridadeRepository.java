package com.matchvagas.backend.repository;

import com.matchvagas.backend.entity.Escolaridades;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface EscolaridadeRepository extends JpaRepository<Escolaridades, Long> {
    
}
