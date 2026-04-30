package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.CandidaturaRequestDTO;
import com.matchvagas.backend.dto.CandidaturaResponseDTO;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Candidatura;
import com.matchvagas.backend.entity.Empresas;
import com.matchvagas.backend.entity.Vagas;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidaturaService {

    private final CandidaturaRepository candidaturasRepository;
    private final CandidatoRepository candidatosRepository;
    private final VagaRepository vagasRepository;
    private final EmpresaRepository empresaRepository;
    private final CandidaturaMapper candidaturaMapper;

    // RF008 — Candidatar-se a uma vaga
    @Transactional
    public CandidaturaResponseDTO candidatar(Long candidatoId, CandidaturaRequestDTO request) {
        Vagas vaga = vagasRepository.findById(request.vagaId())
                .orElseThrow(() -> new BusinessException("Vaga não encontrada"));

        if (!"ATIVA".equalsIgnoreCase(vaga.getStatus().getDescricao()))
            throw new BusinessException("Esta vaga não está mais disponível para candidaturas");

        Candidatos candidato = candidatosRepository.findById(candidatoId)
                .orElseThrow(() -> new BusinessException("Candidato não encontrado"));

        if (candidaturasRepository.existsByCandidatoIdAndVagaId(candidatoId, request.vagaId()))
            throw new BusinessException("Você já se candidatou a esta vaga");

        Candidatura candidatura = new Candidatura();
        candidatura.setCandidato(candidato);
        candidatura.setVaga(vaga);

        return candidaturaMapper.toResponseDTO(candidaturasRepository.save(candidatura));
    }

    // RF009 — Listar todas as candidaturas do candidato
    @Transactional(readOnly = true)
    public List<CandidaturaResponseDTO> findByCandidato(Long candidatoId) {
        return candidaturasRepository.findByCandidatoId(candidatoId)
                .stream().map(candidaturaMapper::toResponseDTO).collect(Collectors.toList());
    }

    // RF009 — Detalhar candidatura garantindo que pertence ao candidato
    @Transactional(readOnly = true)
    public CandidaturaResponseDTO findByIdAndCandidato(Long candidaturaId, Long candidatoId) {
        Candidatura candidatura = candidaturasRepository.findById(candidaturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada"));

        if (!candidatura.getCandidato().getId().equals(candidatoId))
            throw new BusinessException("Você não tem permissão para acessar esta candidatura");

        return candidaturaMapper.toResponseDTO(candidatura);
    }

    // Empresa — listar candidaturas recebidas nas vagas da empresa
    @Transactional(readOnly = true)
    public List<CandidaturaResponseDTO> findByEmpresa(Long usuarioId) {
        Empresas empresa = empresaRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new BusinessException("Nenhuma empresa vinculada a este usuário."));

        return candidaturasRepository.findByVagaEmpresasId(empresa.getId())
                .stream().map(candidaturaMapper::toResponseDTO).collect(Collectors.toList());
    }
}
