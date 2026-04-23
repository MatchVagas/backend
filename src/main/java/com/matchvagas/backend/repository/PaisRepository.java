package com.matchvagas.backend.repository;

import com.matchvagas.backend.entity.Pais;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PaisRepository extends JpaRepository<Pais, Long> {
    Optional<Pais> findByCodigoIso(String codigoIso);
}
