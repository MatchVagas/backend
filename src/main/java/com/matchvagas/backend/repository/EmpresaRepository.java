package com.matchvagas.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matchvagas.backend.entity.Empresas;
import com.matchvagas.backend.entity.RamoAtuacao;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresas, Long> {
    Optional<Empresas> findByCnpjContainingIgnoreCase(String cnpj);
    List<Empresas> findByRamoAtuacao(RamoAtuacao ramoAtuacao);
    Optional<Empresas> findByUsuarioId(Long usuarioId);
}
