package com.matchvagas.backend.repository;

import com.matchvagas.backend.entity.AuditoriaAcesso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaAcessoRepository extends JpaRepository<AuditoriaAcesso, Long> {

    List<AuditoriaAcesso> findByTipoRecursoAndRecursoIdOrderByDataHoraDesc(String tipoRecurso, Long recursoId);

    List<AuditoriaAcesso> findByUsuarioSolicitanteIdOrderByDataHoraDesc(Long usuarioSolicitanteId);
}
