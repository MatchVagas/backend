package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.CandidatoRequestDTO;
import com.matchvagas.backend.dto.CandidatoResponseDTO;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.mapper.CandidatoMapper;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.UsuariosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CandidatoService {

    private final CandidatoRepository candidatoRepository;
    private final UsuariosRepository usuariosRepository;
    private final CandidatoMapper candidatoMapper;

    @Transactional(readOnly = true)
    public CandidatoResponseDTO findByUsuarioId(Long usuarioId) {
        Candidatos candidato = candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de candidato não encontrado para usuário ID: " + usuarioId));
        return candidatoMapper.toResponseDTO(candidato);
    }

    @Transactional
    public CandidatoResponseDTO update(Long usuarioId, CandidatoRequestDTO dto) {
        // 1. Buscar o candidato pelo usuarioId
        Candidatos candidato = candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de candidato não encontrado"));

        Usuarios usuario = candidato.getUsuario();
        if (usuario == null) {
            throw new BusinessException("Candidato não possui usuário associado");
        }

        // 2. Atualizar campos do Usuario
        candidatoMapper.updateUsuarioFromDTO(dto, usuario);
        // Atualizar telefones (requer lógica adicional – simplificamos aqui)
        if (dto.telefones() != null) {
            // Implementar atualização de lista de telefones conforme necessário
            // Exemplo: limpar e adicionar novos
        }

        // 3. Atualizar campos do Candidato
        candidatoMapper.updateCandidatoFromDTO(dto, candidato);

        // 4. Salvar (cascade pode persistir telefones se configurado)
        usuariosRepository.save(usuario);
        Candidatos salvo = candidatoRepository.save(candidato);

        return candidatoMapper.toResponseDTO(salvo);
    }
}