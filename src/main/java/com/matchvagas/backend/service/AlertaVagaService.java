package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.AlertaVagaRequestDTO;
import com.matchvagas.backend.dto.AlertaVagaResponseDTO;
import com.matchvagas.backend.entity.AlertaVaga;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.entity.Vagas;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.repository.AlertaVagaRepository;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.CidadeRepository;
import com.matchvagas.backend.repository.ModalidadeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Alertas de vaga por critérios (Fase 2). O candidato define critérios opcionais; quando
 * uma vaga ATIVA nova casa com um alerta ativo, o dono é notificado (in-app + e-mail via
 * {@link NotificacaoService}, que também emite o push em tempo real).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertaVagaService {

    private static final String TIPO_NOTIFICACAO_ALERTA = "Alerta";

    private final AlertaVagaRepository  alertaRepository;
    private final CandidatoRepository   candidatoRepository;
    private final CidadeRepository      cidadeRepository;
    private final ModalidadeRepository  modalidadeRepository;
    private final NotificacaoService    notificacaoService;

    @Transactional
    public AlertaVagaResponseDTO criar(Long usuarioId, AlertaVagaRequestDTO dto) {
        Candidatos candidato = candidato(usuarioId);

        if (dto.cidadeId() != null && !cidadeRepository.existsById(dto.cidadeId()))
            throw new ResourceNotFoundException("Cidade não encontrada");
        if (dto.modalidadeId() != null && !modalidadeRepository.existsById(dto.modalidadeId()))
            throw new ResourceNotFoundException("Modalidade não encontrada");

        AlertaVaga alerta = new AlertaVaga();
        alerta.setCandidato(candidato);
        alerta.setAreaAtuacao(normalizar(dto.areaAtuacao()));
        alerta.setCidadeId(dto.cidadeId());
        alerta.setModalidadeId(dto.modalidadeId());
        alerta.setSalarioMinimoDesejado(dto.salarioMinimoDesejado());
        alerta.setAtivo(dto.ativo() == null || dto.ativo());

        return toDTO(alertaRepository.save(alerta));
    }

    @Transactional(readOnly = true)
    public List<AlertaVagaResponseDTO> listar(Long usuarioId) {
        Candidatos candidato = candidato(usuarioId);
        return alertaRepository.findByCandidatoIdOrderByDataCriacaoDesc(candidato.getId())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public AlertaVagaResponseDTO alternarAtivo(Long usuarioId, Long alertaId, boolean ativo) {
        AlertaVaga alerta = alertaDoCandidato(usuarioId, alertaId);
        alerta.setAtivo(ativo);
        return toDTO(alertaRepository.save(alerta));
    }

    @Transactional
    public void remover(Long usuarioId, Long alertaId) {
        alertaRepository.delete(alertaDoCandidato(usuarioId, alertaId));
    }

    /**
     * Notifica os candidatos cujos alertas ativos casam com a vaga recém-publicada.
     * Best-effort e idempotente por candidato (um alerta por notificação, sem repetir
     * o mesmo candidato).
     */
    @Transactional(readOnly = true)
    public void notificarNovaVaga(Vagas vaga) {
        Set<Long> jaNotificados = new HashSet<>();
        for (AlertaVaga alerta : alertaRepository.findByAtivoTrue()) {
            if (!casa(alerta, vaga)) continue;

            Usuarios usuario = alerta.getCandidato() != null ? alerta.getCandidato().getUsuario() : null;
            if (usuario == null || !jaNotificados.add(usuario.getId())) continue;

            try {
                notificacaoService.notificarPorTipo(
                        usuario.getId(),
                        "Nova vaga para você",
                        "A vaga \"" + vaga.getTitulo() + "\" combina com um alerta que você criou. Confira!",
                        TIPO_NOTIFICACAO_ALERTA);
            } catch (Exception e) {
                log.warn("Falha ao notificar alerta de vaga (usuário {}): {}", usuario.getId(), e.getMessage());
            }
        }
    }

    /** Verifica se a vaga satisfaz todos os critérios não nulos do alerta. */
    boolean casa(AlertaVaga alerta, Vagas vaga) {
        if (alerta.getAreaAtuacao() != null && !alerta.getAreaAtuacao().isBlank()) {
            if (vaga.getAreaAtuacao() == null
                    || !vaga.getAreaAtuacao().toLowerCase().contains(alerta.getAreaAtuacao().toLowerCase()))
                return false;
        }
        if (alerta.getCidadeId() != null
                && (vaga.getCidade() == null || !alerta.getCidadeId().equals(vaga.getCidade().getId())))
            return false;
        if (alerta.getModalidadeId() != null
                && (vaga.getModalidade() == null || !alerta.getModalidadeId().equals(vaga.getModalidade().getId())))
            return false;
        if (alerta.getSalarioMinimoDesejado() != null
                && (vaga.getSalarioMaximo() == null
                    || vaga.getSalarioMaximo().compareTo(alerta.getSalarioMinimoDesejado()) < 0))
            return false;
        return true;
    }

    private AlertaVaga alertaDoCandidato(Long usuarioId, Long alertaId) {
        Candidatos candidato = candidato(usuarioId);
        return alertaRepository.findByIdAndCandidatoId(alertaId, candidato.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Alerta não encontrado"));
    }

    private Candidatos candidato(Long usuarioId) {
        return candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new BusinessException("Candidato não encontrado"));
    }

    private static String normalizar(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private AlertaVagaResponseDTO toDTO(AlertaVaga a) {
        return new AlertaVagaResponseDTO(
                a.getId(), a.getAreaAtuacao(), a.getCidadeId(), a.getModalidadeId(),
                a.getSalarioMinimoDesejado(), a.isAtivo(), a.getDataCriacao());
    }
}
