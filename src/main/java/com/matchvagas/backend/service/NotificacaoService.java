package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.NotificacoesRequestDTO;
import com.matchvagas.backend.dto.NotificacoesResponseDTO;
import com.matchvagas.backend.dto.PageResponseDTO;
import com.matchvagas.backend.entity.Notificacao;
import com.matchvagas.backend.entity.TipoNotificacao;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.NotificacaoMapper;
import com.matchvagas.backend.repository.NotificacaoRepository;
import com.matchvagas.backend.repository.TipoNotificacaoRepository;
import com.matchvagas.backend.repository.UsuariosRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final UsuariosRepository usuariosRepository;
    private final TipoNotificacaoRepository tipoNotificacaoRepository;
    private final NotificacaoMapper notificacaoMapper;
    private final EmailService emailService;
    private final RealtimeService realtimeService;
    private final AposCommitExecutor aposCommit;

    @Transactional
    public NotificacoesResponseDTO criar(NotificacoesRequestDTO dto) {
        return criar(dto, true);
    }

    /**
     * Cria a notificação in-app e, se {@code enviarEmail} for verdadeiro, também envia o e-mail.
     * O controle existe para cenários de alto volume (ex.: chat), onde um e-mail por evento
     * vira spam — nesses casos o chamador cria o in-app sempre e agenda o e-mail sob sua regra.
     */
    @Transactional
    public NotificacoesResponseDTO criar(NotificacoesRequestDTO dto, boolean enviarEmail) {
        Usuarios usuario = usuariosRepository.findById(dto.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        TipoNotificacao tipo = null;
        if (dto.tipoId() != null) {
            tipo = tipoNotificacaoRepository.findById(dto.tipoId().intValue())
                    .orElseThrow(() -> new ResourceNotFoundException("Tipo de notificação não encontrado"));
        }

        Notificacao notificacao = new Notificacao();
        notificacao.setTitulo(dto.titulo());
        notificacao.setMensagem(dto.mensagem());
        notificacao.setUsuario(usuario);
        notificacao.setTipo(tipo);
        notificacao.setDataEnvio(LocalDateTime.now());
        notificacao.setLida(false);

        Notificacao salva = notificacaoRepository.save(notificacao);
        NotificacoesResponseDTO responseDTO = notificacaoMapper.toDTO(salva);

        // Efeitos externos (push SSE + e-mail) só após o commit — nunca notificam algo
        // que sofra rollback. Best-effort: uma falha aqui não afeta a notificação salva.
        Long destinatarioId = usuario.getId();
        String email = usuario.getEmail();
        String nome = usuario.getNome();
        String titulo = dto.titulo();
        String mensagem = dto.mensagem();
        aposCommit.executar(() -> {
            try {
                realtimeService.enviarPara(destinatarioId, "notificacao", responseDTO);
                if (enviarEmail) {
                    enviarEmailNotificacao(email, nome, titulo, mensagem);
                }
            } catch (Exception e) {
                log.warn("Falha ao entregar notificação pós-commit ao usuário {}: {}",
                        destinatarioId, e.getMessage());
            }
        });

        return responseDTO;
    }

    /**
     * Cria uma notificação (in-app + email) para um usuário, resolvendo o tipo pelo nome.
     * Executa em transação própria (REQUIRES_NEW) para que, quando disparada como efeito
     * colateral de outra operação (ex.: atualização de candidatura), uma eventual falha aqui
     * não desfaça a operação principal. Chamadores devem tratar exceções (best-effort).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notificarPorTipo(Long usuarioId, String titulo, String mensagem, String tipoNome) {
        notificarPorTipo(usuarioId, titulo, mensagem, tipoNome, true);
    }

    /** Variante que permite suprimir o e-mail (mantendo o in-app) — ver {@link #criar(NotificacoesRequestDTO, boolean)}. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notificarPorTipo(Long usuarioId, String titulo, String mensagem, String tipoNome, boolean enviarEmail) {
        Long tipoId = tipoNome == null ? null
                : tipoNotificacaoRepository.findByStatusIgnoreCase(tipoNome)
                        .map(t -> (long) t.getId()).orElse(null);
        criar(new NotificacoesRequestDTO(titulo, mensagem, usuarioId, tipoId), enviarEmail);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<NotificacoesResponseDTO> listarPorUsuario(Long usuarioId, Pageable pageable) {
        return PageResponseDTO.of(
                notificacaoRepository.findByUsuarioIdOrderByDataEnvioDesc(usuarioId, pageable)
                        .map(notificacaoMapper::toDTO));
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<NotificacoesResponseDTO> listarNaoLidas(Long usuarioId, Pageable pageable) {
        return PageResponseDTO.of(
                notificacaoRepository.findByUsuarioIdAndLidaFalseOrderByDataEnvioDesc(usuarioId, pageable)
                        .map(notificacaoMapper::toDTO));
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(Long usuarioId) {
        return notificacaoRepository.countByUsuarioIdAndLidaFalse(usuarioId);
    }

    @Transactional
    public NotificacoesResponseDTO marcarComoLida(Long id, Long usuarioId) {
        Notificacao notificacao = buscarPorIdEUsuario(id, usuarioId);
        notificacao.setLida(true);
        return notificacaoMapper.toDTO(notificacaoRepository.save(notificacao));
    }

    @Transactional
    public void marcarTodasComoLidas(Long usuarioId) {
        List<Notificacao> naoLidas = notificacaoRepository
                .findByUsuarioIdAndLidaFalseOrderByDataEnvioDesc(usuarioId);
        naoLidas.forEach(n -> n.setLida(true));
        notificacaoRepository.saveAll(naoLidas);
    }

    @Transactional
    public void deletar(Long id, Long usuarioId, boolean isAdmin) {
        Notificacao notificacao = notificacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada"));
        if (!isAdmin && !notificacao.getUsuario().getId().equals(usuarioId))
            throw new BusinessException("Você não tem permissão para excluir esta notificação");
        notificacaoRepository.delete(notificacao);
    }

    private Notificacao buscarPorIdEUsuario(Long id, Long usuarioId) {
        Notificacao notificacao = notificacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada"));
        if (!notificacao.getUsuario().getId().equals(usuarioId))
            throw new BusinessException("Você não tem permissão para acessar esta notificação");
        return notificacao;
    }

    private void enviarEmailNotificacao(String email, String nome, String titulo, String mensagem) {
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:24px;border:1px solid #e0e0e0;border-radius:8px;">
                  <h2 style="color:#2563eb;">%s</h2>
                  <p>Olá, <strong>%s</strong>!</p>
                  <p style="white-space:pre-line;">%s</p>
                  <hr style="border:none;border-top:1px solid #e0e0e0;margin:24px 0;">
                  <p style="color:#6b7280;font-size:12px;">MatchVagas — você recebeu esta mensagem pois está cadastrado em nossa plataforma.</p>
                </div>
                """.formatted(titulo, nome, mensagem);
        emailService.enviarEmail(email, titulo, html);
    }
}
