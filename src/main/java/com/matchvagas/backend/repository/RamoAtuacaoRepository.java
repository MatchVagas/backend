package com.matchvagas.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matchvagas.backend.entity.RamoAtuacao;

@Repository
public interface RamoAtuacaoRepository extends JpaRepository<RamoAtuacao, Integer> {

    
}