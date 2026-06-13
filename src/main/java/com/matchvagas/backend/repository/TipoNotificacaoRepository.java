package com.matchvagas.backend.repository;

import com.matchvagas.backend.entity.TipoNotificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoNotificacaoRepository extends JpaRepository<TipoNotificacao, Integer> {
    Optional<TipoNotificacao> findByStatusIgnoreCase(String status);
}
