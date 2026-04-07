package com.matchvagas.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matchvagas.backend.entity.Porte;

@Repository
public interface PorteRepository extends JpaRepository<Porte, Integer> {
    
}
