package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.CandidatoRequestDTO;
import com.matchvagas.backend.dto.CandidatoResponseDTO;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.CandidatoMapper;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.UsuariosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CandidatoService {

    private final CandidatoRepository candidatoRepository;
    private final UsuariosRepository usuariosRepository;
    private final CandidatoMapper candidatoMapper;

    // RF003 — Buscar perfil do candidato pelo ID do usuário autenticado
    @Transactional(readOnly = true)
    public CandidatoResponseDTO findByUsuarioId(Long usuarioId) {
        Candidatos candidato = candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de candidato não encontrado para o usuário ID: " + usuarioId));
        return candidatoMapper.toResponseDTO(candidato);
    }

    @Transactional(readOnly = true)
    public CandidatoResponseDTO findById(Long id) {
        Candidatos candidato = candidatoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidato não encontrado com ID: " + id));
        return candidatoMapper.toResponseDTO(candidato);
    }

    // RF003 — Criar perfil de candidato vinculado ao usuário autenticado
    @Transactional
    public CandidatoResponseDTO create(Long usuarioId, CandidatoRequestDTO dto) {
        // Verifica se já existe perfil para este usuário
        if (candidatoRepository.findByUsuarioId(usuarioId).isPresent()) {
            throw new BusinessException("Já existe um perfil de candidato para este usuário.");
        }

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + usuarioId));

        Candidatos candidato = candidatoMapper.toEntity(dto);
        candidato.setUsuario(usuario);

        return candidatoMapper.toResponseDTO(candidatoRepository.save(candidato));
    }

    // RF003 — Atualizar perfil do candidato
    @Transactional
    public CandidatoResponseDTO update(Long usuarioId, CandidatoRequestDTO dto) {
        Candidatos candidato = candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de candidato não encontrado para o usuário ID: " + usuarioId));

        candidato.setCpf(dto.cpf() != null ? dto.cpf() : candidato.getCpf());
        candidato.setObjetivoProfissional(dto.resumoProfissional());
        candidato.setDisponibilidade(dto.disponibilidade());
        candidato.setPretensaoSalarial(dto.pretensaoSalarial());

        return candidatoMapper.toResponseDTO(candidatoRepository.save(candidato));
    }
}
