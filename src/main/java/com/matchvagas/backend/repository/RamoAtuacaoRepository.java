package com.matchvagas.backend.repository;

import com.matchvagas.backend.entity.RamoAtuacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RamoAtuacaoRepository extends JpaRepository<RamoAtuacao, Long> {}
