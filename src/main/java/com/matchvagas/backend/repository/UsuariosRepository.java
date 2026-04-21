
package com.matchvagas.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.matchvagas.backend.entity.Usuarios;

public interface UsuariosRepository extends JpaRepository<Usuarios, Integer> {

    Optional<Usuarios> findByEmail(String email);

    Optional<Usuarios> findById(Long id);

    boolean existsByEmail(String email);
    
    //Optional<List<Usuarios>> findByAtivo();

}