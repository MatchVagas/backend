package com.matchvagas.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matchvagas.backend.entity.Cidade;
import com.matchvagas.backend.entity.Estado;

@Repository
public interface CidadeRepository extends JpaRepository<Cidade, Integer> {
    List<Cidade> findByEstado(Estado estado);
}
