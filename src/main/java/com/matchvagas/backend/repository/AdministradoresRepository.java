package com.matchvagas.backend.repositoty;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdministradoresRepository extends JpaRepository<Administradores,long>{

}