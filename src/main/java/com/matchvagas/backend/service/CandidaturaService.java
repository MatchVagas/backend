package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.AtualizarCompartilhamentoRequestDTO;
import com.matchvagas.backend.dto.CandidaturaEmpresaResponseDTO;
import com.matchvagas.backend.dto.CandidaturaRequestDTO;
import com.matchvagas.backend.dto.CandidaturaResponseDTO;
import com.matchvagas.backend.dto.ExperienciaResponseDTO;
import com.matchvagas.backend.dto.FormacaoResponseDTO;
import com.matchvagas.backend.dto.HistoricoStatusResponseDTO;
import com.matchvagas.backend.entity.*;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.CandidaturaMapper;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.CandidaturaRepository;
import com.matchvagas.backend.repository.EmpresaRepository;
import com.matchvagas.backend.repository.ExperienciaRepository;
import com.matchvagas.backend.repository.FormacaoRepository;
import com.matchvagas.backend.repository.HistoricoStatusCandidaturaRepository;
import com.matchvagas.backend.repository.StatusCandidaturaRepository;
import com.matchvagas.backend.repository.VagaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidaturaService {

    private static final String TIPO_NOTIFICACAO_CANDIDATURA = "Candidatura";

    private final CandidaturaRepository       candidaturasRepository;
    private final CandidatoRepository         candidatosRepository;
    private final VagaRepository              vagasRepository;
    private final EmpresaRepository           empresaRepository;
    private final CandidaturaMapper           candidaturaMapper;
    private final StatusCandidaturaRepository statusCandidaturaRepository;
    private final ExperienciaRepository       experienciaRepository;
    private final FormacaoRepository          formacaoRepository;
    private final NotificacaoService          notificacaoService;
    private final HistoricoStatusCandidaturaRepository historicoStatusRepository;

    // ── RF008 — Candidatar-se a uma vaga ─────────────────────────────────────

    @Transactional
    public CandidaturaResponseDTO candidatar(Long usuarioId, CandidaturaRequestDTO request) {
        Vagas vaga = vagasRepository.findById(request.vagaId())
                .orElseThrow(() -> new BusinessException("Vaga não encontrada"));

        if (!"ATIVA".equalsIgnoreCase(vaga.getStatus().getDescricao()))
            throw new BusinessException("Esta vaga não está mais disponível para candidaturas");

        Candidatos candidato = candidatosRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new BusinessException("Candidato não encontrado"));

        if (candidaturasRepository.existsByCandidatoIdAndVagaId(candidato.getId(), request.vagaId()))
            throw new BusinessException("Você já se candidatou a esta vaga");

        Candidatura candidatura = new Candidatura();
        candidatura.setCandidato(candidato);
        candidatura.setVaga(vaga);

        // Aplicar preferências de compartilhamento (null = mantém o default da entidade)
        aplicarPreferencias(candidatura, request);

        Candidatura salva = candidaturasRepository.save(candidatura);

        notificar(
                candidato.getUsuario().getId(),
                "Candidatura enviada",
                "Sua candidatura para a vaga \"" + vaga.getTitulo() + "\" foi enviada com sucesso. "
                        + "Acompanhe o andamento pela plataforma.");

        return candidaturaMapper.toResponseDTO(salva);
    }

    // ── RF009 — Listar candidaturas do candidato ──────────────────────────────

    @Transactional(readOnly = true)
    public List<CandidaturaResponseDTO> findByCandidato(Long usuarioId) {
        return candidatosRepository.findByUsuarioId(usuarioId)
                .map(c -> candidaturasRepository.findByCandidatoId(c.getId())
                        .stream().map(candidaturaMapper::toResponseDTO).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    // ── RF009 — Detalhar uma candidatura (somente do próprio candidato) ───────

    @Transactional(readOnly = true)
    public CandidaturaResponseDTO findByIdAndCandidato(Long candidaturaId, Long usuarioId) {
        Candidatura candidatura = candidaturasRepository.findById(candidaturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada"));

        if (!candidatura.getCandidato().getUsuario().getId().equals(usuarioId))
            throw new BusinessException("Você não tem permissão para acessar esta candidatura");

        return candidaturaMapper.toResponseDTO(candidatura);
    }

    // ── Atualizar preferências de compartilhamento ────────────────────────────

    @Transactional
    public CandidaturaResponseDTO atualizarCompartilhamento(
            Long candidaturaId, Long usuarioId, AtualizarCompartilhamentoRequestDTO dto) {

        Candidatura candidatura = candidaturasRepository.findById(candidaturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada"));

        if (!candidatura.getCandidato().getUsuario().getId().equals(usuarioId))
            throw new BusinessException("Você não tem permissão para alterar esta candidatura");

        candidatura.setCompartilharObjetivoProfissional(dto.compartilharObjetivoProfissional());
        candidatura.setCompartilharDisponibilidade(dto.compartilharDisponibilidade());
        candidatura.setCompartilharPretensaoSalarial(dto.compartilharPretensaoSalarial());
        candidatura.setCompartilharCurriculo(dto.compartilharCurriculo());
        candidatura.setCompartilharExperiencias(dto.compartilharExperiencias());
        candidatura.setCompartilharFormacoes(dto.compartilharFormacoes());
        candidatura.setCompartilharTelefone(dto.compartilharTelefone());
        candidatura.setCompartilharEndereco(dto.compartilharEndereco());

        return candidaturaMapper.toResponseDTO(candidaturasRepository.save(candidatura));
    }

    // ── Empresa — listar candidaturas recebidas com dados filtrados ───────────

    @Transactional(readOnly = true)
    public List<CandidaturaEmpresaResponseDTO> findByEmpresa(Long usuarioId) {
        Empresas empresa = empresaRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new BusinessException("Nenhuma empresa vinculada a este usuário."));

        return candidaturasRepository.findByVagaEmpresasId(empresa.getId())
                .stream()
                .map(this::toEmpresaResponseDTO)
                .collect(Collectors.toList());
    }

    // ── Empresa — candidatos de uma vaga específica ───────────────────────────

    @Transactional(readOnly = true)
    public List<CandidaturaEmpresaResponseDTO> findByVagaAndEmpresa(Long vagaId, Long usuarioId) {
        Empresas empresa = empresaRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new BusinessException("Nenhuma empresa vinculada a este usuário."));

        Vagas vaga = vagasRepository.findById(vagaId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaga não encontrada com ID: " + vagaId));

        if (!vaga.getEmpresas().getId().equals(empresa.getId()))
            throw new BusinessException("Esta vaga não pertence à sua empresa");

        return candidaturasRepository.findByVagaId(vagaId)
                .stream()
                .map(this::toEmpresaResponseDTO)
                .collect(Collectors.toList());
    }

    // ── Empresa — detalhar uma candidatura específica ─────────────────────────

    @Transactional(readOnly = true)
    public CandidaturaEmpresaResponseDTO findByIdAndEmpresa(Long candidaturaId, Long usuarioId) {
        Empresas empresa = empresaRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new BusinessException("Nenhuma empresa vinculada a este usuário."));

        Candidatura candidatura = candidaturasRepository.findById(candidaturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada"));

        if (!candidatura.getVaga().getEmpresas().getId().equals(empresa.getId()))
            throw new BusinessException("Esta candidatura não pertence a uma vaga da sua empresa");

        return toEmpresaResponseDTO(candidatura);
    }

    @Transactional
    public CandidaturaEmpresaResponseDTO atualizarStatusEmpresa(Long candidaturaId, Long statusId, Long usuarioId) {
        Empresas empresa = empresaRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new BusinessException("Nenhuma empresa vinculada a este usuário."));

        Candidatura candidatura = candidaturasRepository.findById(candidaturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada"));

        if (!candidatura.getVaga().getEmpresas().getId().equals(empresa.getId()))
            throw new BusinessException("Esta candidatura não pertence a uma vaga da sua empresa");

        StatusCandidatura novoStatus = statusCandidaturaRepository.findById(statusId)
                .orElseThrow(() -> new ResourceNotFoundException("Status não encontrado"));

        StatusCandidatura statusAnterior = candidatura.getStatus();

        // Guard de no-op: status inalterado → não persiste, não registra histórico, não notifica
        if (statusAnterior != null && statusAnterior.getId() == novoStatus.getId()) {
            return toEmpresaResponseDTO(candidatura);
        }

        validarTransicao(statusAnterior, novoStatus);

        candidatura.setStatus(novoStatus);
        CandidaturaEmpresaResponseDTO response = toEmpresaResponseDTO(candidaturasRepository.save(candidatura));

        registrarHistorico(candidatura, statusAnterior, novoStatus, empresa.getUsuario());

        notificar(
                candidatura.getCandidato().getUsuario().getId(),
                "Status da candidatura atualizado",
                "O status da sua candidatura para a vaga \"" + candidatura.getVaga().getTitulo()
                        + "\" foi atualizado para: " + novoStatus.getStatus() + ".");

        return response;
    }

    // ── Empresa — histórico de mudanças de status de uma candidatura ──────────

    @Transactional(readOnly = true)
    public List<HistoricoStatusResponseDTO> listarHistoricoEmpresa(Long candidaturaId, Long usuarioId) {
        Empresas empresa = empresaRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new BusinessException("Nenhuma empresa vinculada a este usuário."));

        Candidatura candidatura = candidaturasRepository.findById(candidaturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada"));

        if (!candidatura.getVaga().getEmpresas().getId().equals(empresa.getId()))
            throw new BusinessException("Esta candidatura não pertence a uma vaga da sua empresa");

        return historicoStatusRepository.findByCandidaturaIdOrderByDataHoraDesc(candidaturaId)
                .stream().map(this::toHistoricoDTO).collect(Collectors.toList());
    }

    private HistoricoStatusResponseDTO toHistoricoDTO(HistoricoStatusCandidatura h) {
        return new HistoricoStatusResponseDTO(
                h.getId(),
                h.getStatusAnterior() != null ? h.getStatusAnterior().getStatus() : null,
                h.getStatusNovo().getStatus(),
                h.getUsuario().getId(),
                h.getUsuario().getNome(),
                h.getDataHora());
    }

    // ── Interno: valida se a transição de status é permitida pelo fluxo ───────

    private void validarTransicao(StatusCandidatura statusAtual, StatusCandidatura novoStatus) {
        // Sem status de origem (candidatura recém-criada) → qualquer destino é válido
        if (statusAtual == null) return;

        Optional<FluxoStatusCandidatura> origem = FluxoStatusCandidatura.fromNome(statusAtual.getStatus());
        Optional<FluxoStatusCandidatura> destino = FluxoStatusCandidatura.fromNome(novoStatus.getStatus());

        // Status fora do fluxo conhecido (customizado) → não há regra a aplicar
        if (origem.isEmpty() || destino.isEmpty()) return;

        if (!origem.get().podeTransicionarPara(destino.get())) {
            String motivo = origem.get().ehTerminal()
                    ? "A candidatura está em um status final (\"" + statusAtual.getStatus()
                            + "\") e não pode ser reaberta."
                    : "Não é permitido mudar o status de \"" + statusAtual.getStatus()
                            + "\" para \"" + novoStatus.getStatus() + "\".";
            throw new BusinessException(motivo);
        }
    }

    // ── Interno: registra a mudança de status na trilha de auditoria ──────────

    private void registrarHistorico(Candidatura candidatura, StatusCandidatura statusAnterior,
                                    StatusCandidatura statusNovo, Usuarios usuario) {
        HistoricoStatusCandidatura historico = new HistoricoStatusCandidatura();
        historico.setCandidatura(candidatura);
        historico.setStatusAnterior(statusAnterior);
        historico.setStatusNovo(statusNovo);
        historico.setUsuario(usuario);
        historicoStatusRepository.save(historico);
    }

    // ── Interno: notifica o candidato (best-effort — não afeta a operação principal) ──

    private void notificar(Long usuarioId, String titulo, String mensagem) {
        try {
            notificacaoService.notificarPorTipo(usuarioId, titulo, mensagem, TIPO_NOTIFICACAO_CANDIDATURA);
        } catch (Exception e) {
            log.warn("Falha ao gerar notificação para o usuário {}: {}", usuarioId, e.getMessage());
        }
    }

    // ── Interno: monta o DTO filtrado para a empresa ──────────────────────────

    private CandidaturaEmpresaResponseDTO toEmpresaResponseDTO(Candidatura c) {
        Candidatos cand = c.getCandidato();

        String objetivoProfissional  = c.isCompartilharObjetivoProfissional() ? cand.getObjetivoProfissional() : null;
        String disponibilidade       = c.isCompartilharDisponibilidade()      ? cand.getDisponibilidade()      : null;
        var    pretensaoSalarial     = c.isCompartilharPretensaoSalarial()    ? cand.getPretensaoSalarial()    : null;

        String curriculoNome   = null;
        String curriculoCaminho = null;
        if (c.isCompartilharCurriculo() && cand.getCurriculo() != null) {
            curriculoNome    = cand.getCurriculo().getNomeArquivo();
            curriculoCaminho = cand.getCurriculo().getCaminhoArquivo();
        }

        List<ExperienciaResponseDTO> experiencias = null;
        if (c.isCompartilharExperiencias()) {
            experiencias = experienciaRepository.findByCandidatoId(cand.getId()).stream()
                    .map(e -> new ExperienciaResponseDTO(
                            e.getId(), cand.getId(), e.getEmpresa(), e.getCargo(),
                            e.getDescricao(), e.getDataInicio(), e.getDataFim()))
                    .collect(Collectors.toList());
        }

        List<FormacaoResponseDTO> formacoes = null;
        if (c.isCompartilharFormacoes()) {
            formacoes = formacaoRepository.findByCandidatoId(cand.getId()).stream()
                    .map(f -> new FormacaoResponseDTO(
                            f.getId(), cand.getId(), f.getInstituicao(), f.getCurso(),
                            f.getNivel(), f.getDataInicio(), f.getDataFim()))
                    .collect(Collectors.toList());
        }

        List<String> telefones = null;
        if (c.isCompartilharTelefone() && cand.getUsuario() != null
                && cand.getUsuario().getTelefones() != null) {
            telefones = cand.getUsuario().getTelefones().stream()
                    .map(t -> t.getNumero()
                            + (t.getTipoTelefone() != null
                               ? " (" + t.getTipoTelefone().getNome() + ")" : "")
                            + (t.isWpp() ? " [WhatsApp]" : ""))
                    .collect(Collectors.toList());
        }

        String endereco = null;
        if (c.isCompartilharEndereco() && cand.getEndereco() != null) {
            Endereco e = cand.getEndereco();
            endereco = e.getLogradouro() + ", " + e.getNumero()
                    + (e.getCompleto() != null && !e.getCompleto().isBlank()
                       ? " — " + e.getCompleto() : "")
                    + " — " + e.getBairro()
                    + ", " + e.getCidade() + "/" + e.getEstado()
                    + " — CEP: " + e.getCep();
        }

        String statusDesc = c.getStatus() != null ? c.getStatus().getStatus() : null;

        return new CandidaturaEmpresaResponseDTO(
                c.getId(),
                c.getVaga().getId(),
                c.getVaga().getTitulo(),
                c.getDataCandidatura(),
                statusDesc,
                cand.getId(),
                cand.getUsuario().getNome(),
                cand.getUsuario() != null ? cand.getUsuario().getEmail() : null,
                objetivoProfissional,
                disponibilidade,
                pretensaoSalarial,
                curriculoNome,
                curriculoCaminho,
                experiencias,
                formacoes,
                telefones,
                endereco
        );
    }

    // ── Interno: aplica preferências do request à entidade ───────────────────

    private void aplicarPreferencias(Candidatura candidatura, CandidaturaRequestDTO req) {
        if (req.compartilharObjetivoProfissional() != null)
            candidatura.setCompartilharObjetivoProfissional(req.compartilharObjetivoProfissional());
        if (req.compartilharDisponibilidade() != null)
            candidatura.setCompartilharDisponibilidade(req.compartilharDisponibilidade());
        if (req.compartilharPretensaoSalarial() != null)
            candidatura.setCompartilharPretensaoSalarial(req.compartilharPretensaoSalarial());
        if (req.compartilharCurriculo() != null)
            candidatura.setCompartilharCurriculo(req.compartilharCurriculo());
        if (req.compartilharExperiencias() != null)
            candidatura.setCompartilharExperiencias(req.compartilharExperiencias());
        if (req.compartilharFormacoes() != null)
            candidatura.setCompartilharFormacoes(req.compartilharFormacoes());
        if (req.compartilharTelefone() != null)
            candidatura.setCompartilharTelefone(req.compartilharTelefone());
        if (req.compartilharEndereco() != null)
            candidatura.setCompartilharEndereco(req.compartilharEndereco());
    }
}
