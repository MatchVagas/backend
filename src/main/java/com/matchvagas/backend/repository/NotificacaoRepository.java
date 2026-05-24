package com.matchvagas.backend.repository;

import com.matchvagas.backend.entity.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    List<Notificacao> findByUsuarioIdOrderByDataEnvioDesc(Long usuarioId);
    List<Notificacao> findByUsuarioIdAndLidaFalseOrderByDataEnvioDesc(Long usuarioId);
    long countByUsuarioIdAndLidaFalse(Long usuarioId);
}
