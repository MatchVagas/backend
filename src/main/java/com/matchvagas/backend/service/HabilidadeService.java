package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.HabilidadeRequestDTO;
import com.matchvagas.backend.dto.HabilidadeResponseDTO;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Habilidade;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.HabilidadeMapper;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.service.embedding.IndexacaoEmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HabilidadeService {

    private final CandidatoRepository candidatoRepository;
    private final HabilidadeMapper habilidadeMapper;
    private final IndexacaoEmbeddingService indexacaoEmbeddingService;
    private final AposCommitExecutor aposCommitExecutor;

    @Transactional(readOnly = true)
    public List<HabilidadeResponseDTO> listar(Long usuarioId) {
        return buscarCandidatoPorUsuario(usuarioId).getHabilidades()
                .stream()
                .map(habilidadeMapper::toResponseDTO)
                .toList();
    }

    @Transactional
    public HabilidadeResponseDTO adicionar(Long usuarioId, HabilidadeRequestDTO dto) {
        Candidatos candidato = buscarCandidatoPorUsuario(usuarioId);

        boolean nomeJaExiste = candidato.getHabilidades().stream()
                .anyMatch(h -> h.getNome().equalsIgnoreCase(dto.nome()));
        if (nomeJaExiste) {
            throw new BusinessException("Habilidade '" + dto.nome() + "' já cadastrada para este candidato.");
        }

        Habilidade habilidade = habilidadeMapper.toEntity(dto);
        candidato.getHabilidades().add(habilidade);
        candidatoRepository.save(candidato);
        reindexar(candidato);
        return habilidadeMapper.toResponseDTO(habilidade);
    }

    @Transactional
    public HabilidadeResponseDTO atualizar(Long usuarioId, String nomeAtual, HabilidadeRequestDTO dto) {
        Candidatos candidato = buscarCandidatoPorUsuario(usuarioId);

        Habilidade habilidade = candidato.getHabilidades().stream()
                .filter(h -> h.getNome().equalsIgnoreCase(nomeAtual))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Habilidade '" + nomeAtual + "' não encontrada."));

        if (!nomeAtual.equalsIgnoreCase(dto.nome())) {
            boolean nomeJaExiste = candidato.getHabilidades().stream()
                    .anyMatch(h -> h.getNome().equalsIgnoreCase(dto.nome()));
            if (nomeJaExiste) {
                throw new BusinessException("Habilidade '" + dto.nome() + "' já cadastrada para este candidato.");
            }
        }

        habilidade.setNome(dto.nome());
        habilidade.setNivel(dto.nivel());
        candidatoRepository.save(candidato);
        reindexar(candidato);
        return habilidadeMapper.toResponseDTO(habilidade);
    }

    @Transactional
    public void remover(Long usuarioId, String nome) {
        Candidatos candidato = buscarCandidatoPorUsuario(usuarioId);

        boolean removido = candidato.getHabilidades()
                .removeIf(h -> h.getNome().equalsIgnoreCase(nome));
        if (!removido) {
            throw new ResourceNotFoundException("Habilidade '" + nome + "' não encontrada.");
        }

        candidatoRepository.save(candidato);
        reindexar(candidato);
    }

    private Candidatos buscarCandidatoPorUsuario(Long usuarioId) {
        return candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de candidato não encontrado para o usuário ID: " + usuarioId));
    }

    private void reindexar(Candidatos candidato) {
        aposCommitExecutor.executar(() -> indexacaoEmbeddingService.indexarCandidato(candidato));
    }
}
