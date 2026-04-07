package com.matchvagas.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matchvagas.backend.entity.Formacao;

@Repository
public interface FormacaoRepository extends JpaRepository<Formacao, Long> {
    List<Formacao> findByCandidatoId(Long candidatoId);    
} 