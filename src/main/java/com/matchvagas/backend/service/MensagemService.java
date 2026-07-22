package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.MensagemRequestDTO;
import com.matchvagas.backend.dto.MensagemResponseDTO;
import com.matchvagas.backend.dto.PageResponseDTO;
import com.matchvagas.backend.entity.Candidatura;
import com.matchvagas.backend.entity.Empresas;
import com.matchvagas.backend.entity.Mensagem;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.repository.CandidaturaRepository;
import com.matchvagas.backend.repository.MensagemRepository;
import com.matchvagas.backend.repository.UsuariosRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Conversa empresa ↔ candidato atrelada a uma candidatura (Fase 2).
 *
 * <p>Não há entidade de "conversa": a própria candidatura é o thread. Os dois
 * participantes são o candidato dono da candidatura e o usuário gestor da empresa
 * dona da vaga; qualquer outro usuário é barrado por {@link #resolverParticipante}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MensagemService {

    private static final String TIPO_NOTIFICACAO_MENSAGEM = "Mensagem";

    private final MensagemRepository    mensagemRepository;
    private final CandidaturaRepository candidaturaRepository;
    private final UsuariosRepository    usuariosRepository;
    private final NotificacaoService    notificacaoService;
    private final RealtimeService       realtimeService;

    private enum Papel { CANDIDATO, EMPRESA }

    // ── Enviar mensagem ───────────────────────────────────────────────────────

    @Transactional
    public MensagemResponseDTO enviar(Long usuarioId, MensagemRequestDTO dto) {
        Candidatura candidatura = buscarCandidatura(dto.candidaturaId());
        Papel remetentePapel = resolverParticipante(candidatura, usuarioId);

        Usuarios remetente = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Long destinatarioId = destinatarioUsuarioId(candidatura, remetentePapel);

        // Anti-spam: só manda e-mail quando o destinatário já leu tudo até aqui, isto é,
        // no começo de uma rajada. Se ele ainda tem mensagens não lidas nesta conversa,
        // cria só o in-app (evita um e-mail por mensagem). A contagem é feita ANTES do save.
        boolean destinatarioEstaEmDia = destinatarioId != null
                && mensagemRepository.countByCandidaturaIdAndRemetenteIdNotAndLidaFalse(
                        candidatura.getId(), destinatarioId) == 0;

        Mensagem mensagem = new Mensagem();
        mensagem.setCandidatura(candidatura);
        mensagem.setRemetente(remetente);
        mensagem.setConteudo(dto.conteudo().trim());

        Mensagem salva = mensagemRepository.save(mensagem);
        MensagemResponseDTO response = toResponseDTO(salva, candidatura);

        // Push em tempo real ao destinatário — atualiza a conversa aberta na hora.
        realtimeService.enviarPara(destinatarioId, "mensagem", response);

        notificarDestinatario(candidatura, destinatarioId, remetente, destinatarioEstaEmDia);

        return response;
    }

    // ── Listar a conversa (mais antigas primeiro) ─────────────────────────────

    @Transactional(readOnly = true)
    public PageResponseDTO<MensagemResponseDTO> listar(Long usuarioId, Long candidaturaId, Pageable pageable) {
        Candidatura candidatura = buscarCandidatura(candidaturaId);
        resolverParticipante(candidatura, usuarioId); // autoriza

        return PageResponseDTO.of(
                mensagemRepository.findByCandidaturaIdOrderByDataEnvioAsc(candidaturaId, pageable)
                        .map(m -> toResponseDTO(m, candidatura)));
    }

    // ── Marcar como lidas as mensagens recebidas nesta conversa ───────────────

    @Transactional
    public int marcarComoLidas(Long usuarioId, Long candidaturaId) {
        Candidatura candidatura = buscarCandidatura(candidaturaId);
        resolverParticipante(candidatura, usuarioId);

        List<Mensagem> naoLidas = mensagemRepository
                .findByCandidaturaIdAndRemetenteIdNotAndLidaFalse(candidaturaId, usuarioId);
        naoLidas.forEach(m -> m.setLida(true));
        mensagemRepository.saveAll(naoLidas);
        return naoLidas.size();
    }

    // ── Contagens de não lidas ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public long contarNaoLidas(Long usuarioId, Long candidaturaId) {
        Candidatura candidatura = buscarCandidatura(candidaturaId);
        resolverParticipante(candidatura, usuarioId);
        return mensagemRepository.countByCandidaturaIdAndRemetenteIdNotAndLidaFalse(candidaturaId, usuarioId);
    }

    @Transactional(readOnly = true)
    public long contarNaoLidasTotais(Long usuarioId) {
        return mensagemRepository.contarNaoLidasPorParticipante(usuarioId);
    }

    // ── Internos ──────────────────────────────────────────────────────────────

    private Candidatura buscarCandidatura(Long candidaturaId) {
        return candidaturaRepository.findById(candidaturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada"));
    }

    /** Identifica de que lado o usuário está na conversa; barra quem não participa. */
    private Papel resolverParticipante(Candidatura candidatura, Long usuarioId) {
        Long candidatoUsuarioId = candidatura.getCandidato().getUsuario().getId();
        if (candidatoUsuarioId.equals(usuarioId)) {
            return Papel.CANDIDATO;
        }
        Empresas empresa = candidatura.getVaga().getEmpresas();
        if (empresa != null && empresa.getUsuario() != null
                && empresa.getUsuario().getId().equals(usuarioId)) {
            return Papel.EMPRESA;
        }
        throw new BusinessException("Você não participa desta conversa");
    }

    /** Usuário que deve receber a mensagem (a outra parte da conversa). */
    private Long destinatarioUsuarioId(Candidatura candidatura, Papel remetentePapel) {
        return remetentePapel == Papel.CANDIDATO
                ? empresaUsuarioId(candidatura)
                : candidatura.getCandidato().getUsuario().getId();
    }

    /** Notifica a outra parte (best-effort — não desfaz o envio da mensagem). */
    private void notificarDestinatario(Candidatura candidatura, Long destinatarioUsuarioId,
                                       Usuarios remetente, boolean enviarEmail) {
        if (destinatarioUsuarioId == null) {
            return; // empresa sem usuário gestor vinculado — nada a notificar
        }

        try {
            notificacaoService.notificarPorTipo(
                    destinatarioUsuarioId,
                    "Nova mensagem",
                    remetente.getNome() + " enviou uma mensagem sobre a vaga \""
                            + candidatura.getVaga().getTitulo() + "\".",
                    TIPO_NOTIFICACAO_MENSAGEM,
                    enviarEmail);
        } catch (Exception e) {
            log.warn("Falha ao notificar destinatário da mensagem (candidatura {}): {}",
                    candidatura.getId(), e.getMessage());
        }
    }

    private Long empresaUsuarioId(Candidatura candidatura) {
        Empresas empresa = candidatura.getVaga().getEmpresas();
        return (empresa != null && empresa.getUsuario() != null) ? empresa.getUsuario().getId() : null;
    }

    private MensagemResponseDTO toResponseDTO(Mensagem m, Candidatura candidatura) {
        Long candidatoUsuarioId = candidatura.getCandidato().getUsuario().getId();
        String papel = m.getRemetente().getId().equals(candidatoUsuarioId)
                ? Papel.CANDIDATO.name() : Papel.EMPRESA.name();

        return new MensagemResponseDTO(
                m.getId(),
                candidatura.getId(),
                m.getRemetente().getId(),
                m.getRemetente().getNome(),
                papel,
                m.getConteudo(),
                m.getDataEnvio(),
                m.isLida());
    }
}
