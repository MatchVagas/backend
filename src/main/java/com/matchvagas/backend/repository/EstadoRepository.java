package com.matchvagas.backend.repository;

import com.matchvagas.backend.entity.Estado;
import com.matchvagas.backend.entity.Pais;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EstadoRepository extends JpaRepository<Estado, Long> {
    List<Estado> findByPais(Pais pais);
    List<Estado> findByPaisId(Long paisId);
}
