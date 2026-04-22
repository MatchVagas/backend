package com.matchvagas.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matchvagas.backend.entity.Administradores;
import com.matchvagas.backend.entity.Departamentos;

@Repository
public interface AdministradoresRepository extends JpaRepository<Administradores, Long> {
    List<Administradores> findByUsuarioId(Long usuarioId);
    List<Administradores> findByDepartamento(Departamentos departamento);
}