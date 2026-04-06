
package com.matchvagas.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.matchvagas.backend.entity.Usuarios;

interface UsuariosRepository extends JpaRepository<Usuarios, Integer> {

    Optional<Usuarios> findByEmail(String email);
    
    //Optional<List<Usuarios>> findByAtivo();

}