package com.matchvagas.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.matchvagas.backend.entity.Telefones;

public interface TelefoneRepository extends JpaRepository<Telefones, Integer> {
    
    Optional<Telefones> findByNumero(String numero);
    
}
