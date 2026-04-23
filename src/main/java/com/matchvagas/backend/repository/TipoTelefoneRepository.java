package com.matchvagas.backend.repository;

import com.matchvagas.backend.entity.TipoTelefone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoTelefoneRepository extends JpaRepository<TipoTelefone, Long> {}
