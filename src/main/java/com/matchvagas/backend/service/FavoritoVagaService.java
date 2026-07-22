package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.PageResponseDTO;
import com.matchvagas.backend.dto.VagaResponseDTO;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.FavoritoVaga;
import com.matchvagas.backend.entity.Vagas;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.VagasMapper;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.FavoritoVagaRepository;
import com.matchvagas.backend.repository.VagaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vagas favoritadas pelo candidato (Fase 2). Salvar é idempotente; listar devolve as
 * vagas em si (para o candidato ver o que guardou), respeitando o mapeamento padrão.
 */
@Service
@RequiredArgsConstructor
public class FavoritoVagaService {

    private final FavoritoVagaRepository favoritoRepository;
    private final CandidatoRepository    candidatoRepository;
    private final VagaRepository         vagaRepository;
    private final VagasMapper            vagasMapper;

    @Transactional
    public VagaResponseDTO favoritar(Long usuarioId, Long vagaId) {
        Candidatos candidato = candidato(usuarioId);
        Vagas vaga = vagaRepository.findById(vagaId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaga não encontrada com ID: " + vagaId));

        // Idempotente: favoritar de novo não duplica nem falha.
        if (!favoritoRepository.existsByCandidatoIdAndVagaId(candidato.getId(), vagaId)) {
            FavoritoVaga favorito = new FavoritoVaga();
            favorito.setCandidato(candidato);
            favorito.setVaga(vaga);
            favoritoRepository.save(favorito);
        }

        return vagasMapper.toDTO(vaga);
    }

    @Transactional
    public void desfavoritar(Long usuarioId, Long vagaId) {
        Candidatos candidato = candidato(usuarioId);
        FavoritoVaga favorito = favoritoRepository
                .findByCandidatoIdAndVagaId(candidato.getId(), vagaId)
                .orElseThrow(() -> new ResourceNotFoundException("Esta vaga não está nos seus favoritos"));
        favoritoRepository.delete(favorito);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<VagaResponseDTO> listar(Long usuarioId, Pageable pageable) {
        Candidatos candidato = candidato(usuarioId);
        return PageResponseDTO.of(
                favoritoRepository.findByCandidatoIdOrderByDataFavoritadoDesc(candidato.getId(), pageable)
                        .map(f -> vagasMapper.toDTO(f.getVaga())));
    }

    @Transactional(readOnly = true)
    public boolean estaFavoritada(Long usuarioId, Long vagaId) {
        return favoritoRepository.existsByCandidatoIdAndVagaId(candidato(usuarioId).getId(), vagaId);
    }

    private Candidatos candidato(Long usuarioId) {
        return candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new BusinessException("Candidato não encontrado"));
    }
}
