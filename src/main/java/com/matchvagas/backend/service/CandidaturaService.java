package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.AtualizarCompartilhamentoRequestDTO;
import com.matchvagas.backend.dto.CandidaturaEmpresaResponseDTO;
import com.matchvagas.backend.dto.CandidaturaRequestDTO;
import com.matchvagas.backend.dto.CandidaturaResponseDTO;
import com.matchvagas.backend.entity.*;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.CandidaturaMapper;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.CandidaturaRepository;
import com.matchvagas.backend.repository.EmpresaRepository;
import com.matchvagas.backend.repository.VagaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidaturaService {

    private final CandidaturaRepository candidaturasRepository;
    private final CandidatoRepository   candidatosRepository;
    private final VagaRepository        vagasRepository;
    private final EmpresaRepository     empresaRepository;
    private final CandidaturaMapper     candidaturaMapper;

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

        return candidaturaMapper.toResponseDTO(candidaturasRepository.save(candidatura));
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

        // Experiências e Formações: placeholder — serão expandidos quando as
        // entidades Experiencia/Formacao estiverem vinculadas a Candidatos
        String experienciasInfo = c.isCompartilharExperiencias() ? "Disponível" : null;
        String formacoesInfo    = c.isCompartilharFormacoes()    ? "Disponível" : null;

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
                objetivoProfissional,
                disponibilidade,
                pretensaoSalarial,
                curriculoNome,
                curriculoCaminho,
                experienciasInfo,
                formacoesInfo,
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
