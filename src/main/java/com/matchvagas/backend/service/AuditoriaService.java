package com.matchvagas.backend.service;

import com.matchvagas.backend.entity.AuditoriaAcesso;
import com.matchvagas.backend.repository.AuditoriaAcessoRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Registra acessos a dados pessoais na trilha de auditoria (LGPD Art. 37).
 * O IP de origem é obtido do contexto da requisição atual, sem necessidade de
 * propagá-lo pelas assinaturas dos métodos de negócio.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaAcessoRepository repository;

    @Transactional
    public void registrar(Long usuarioSolicitanteId, Long recursoId, String tipoRecurso, String operacao) {
        AuditoriaAcesso registro = new AuditoriaAcesso();
        registro.setUsuarioSolicitanteId(usuarioSolicitanteId);
        registro.setRecursoId(recursoId);
        registro.setTipoRecurso(tipoRecurso);
        registro.setOperacao(operacao);
        registro.setIpOrigem(ipOrigem());
        repository.save(registro);
    }

    private String ipOrigem() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return null;
    }
}
