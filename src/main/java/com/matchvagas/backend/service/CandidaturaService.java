package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.CandidaturaRequestDTO;
import com.matchvagas.backend.dto.CandidaturaResponseDTO;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Candidatura;
import com.matchvagas.backend.entity.Vagas;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.mapper.CandidatoMapper;
import com.matchvagas.backend.mapper.CandidaturaMapper;
import com.matchvagas.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CandidaturaService {

    private final CandidaturaRepository candidaturasRepository;
    private final CandidatoRepository candidatosRepository;
    private final VagaRepository vagasRepository;
    private final CandidaturaMapper candidaturaMapper;

    @Transactional
    public CandidaturaResponseDTO candidatar(Long candidatoId, CandidaturaRequestDTO request) {
        // 1. Verificar se a vaga existe e está ativa
        Vagas vaga = vagasRepository.findById(request.vagaId())
                .orElseThrow(() -> new BusinessException("Vaga não encontrada"));

        // Ajuste conforme o campo real de status da vaga (ex: isAtiva() ou getStatus())
        if (!"ATIVA".equalsIgnoreCase(vaga.getStatus().getDescricao())) {
            throw new BusinessException("Esta vaga não está mais disponível para candidaturas");
        }

        // 2. Verificar se o candidato existe
        Candidatos candidato = candidatosRepository.findById(candidatoId)
                .orElseThrow(() -> new BusinessException("Candidato não encontrado"));

        // 3. Verificar se já existe candidatura (usa restrição unique do banco, mas previne antes)
        if (candidaturasRepository.existsByCandidatoIdAndVagaId(candidatoId, request.vagaId())) {
            throw new BusinessException("Você já se candidatou a esta vaga");
        }

        // 4. Criar a candidatura
        Candidatura candidatura = new Candidatura();
        candidatura.setCandidato(candidato);
        candidatura.setVaga(vaga);
        // dataCandidatura e status são definidos pelo @PrePersist

        Candidatura salva = candidaturasRepository.save(candidatura);

        // 5. Retornar DTO mapeado
        return candidaturaMapper.toResponseDTO(salva);
    }
}