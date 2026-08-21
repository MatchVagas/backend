package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.ExperienciaRequestDTO;
import com.matchvagas.backend.dto.ExperienciaResponseDTO;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Experiencia;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.ExperienciaRepository;
import com.matchvagas.backend.service.embedding.IndexacaoEmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExperienciaService {

    private final ExperienciaRepository experienciaRepository;
    private final CandidatoRepository candidatoRepository;
    private final IndexacaoEmbeddingService indexacaoEmbeddingService;
    private final AposCommitExecutor aposCommitExecutor;

    @Transactional(readOnly = true)
    public List<ExperienciaResponseDTO> listar(Long usuarioId) {
        Candidatos candidato = buscarCandidatoPorUsuario(usuarioId);
        return experienciaRepository.findByCandidatoId(candidato.getId())
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional
    public ExperienciaResponseDTO adicionar(Long usuarioId, ExperienciaRequestDTO dto) {
        Candidatos candidato = buscarCandidatoPorUsuario(usuarioId);
        Experiencia experiencia = new Experiencia();
        experiencia.setEmpresa(dto.empresa());
        experiencia.setCargo(dto.cargo());
        experiencia.setDescricao(dto.descricao());
        experiencia.setDataInicio(dto.dataInicio());
        experiencia.setDataFim(dto.dataFim());
        experiencia.setCandidato(candidato);
        Experiencia salva = experienciaRepository.save(experiencia);
        reindexar(candidato);
        return toResponseDTO(salva);
    }

    @Transactional
    public ExperienciaResponseDTO atualizar(Long usuarioId, Long id, ExperienciaRequestDTO dto) {
        Candidatos candidato = buscarCandidatoPorUsuario(usuarioId);
        Experiencia experiencia = buscarExperienciaDoCandidato(id, candidato.getId());
        experiencia.setEmpresa(dto.empresa());
        experiencia.setCargo(dto.cargo());
        experiencia.setDescricao(dto.descricao());
        experiencia.setDataInicio(dto.dataInicio());
        experiencia.setDataFim(dto.dataFim());
        Experiencia salva = experienciaRepository.save(experiencia);
        reindexar(candidato);
        return toResponseDTO(salva);
    }

    @Transactional
    public void remover(Long usuarioId, Long id) {
        Candidatos candidato = buscarCandidatoPorUsuario(usuarioId);
        Experiencia experiencia = buscarExperienciaDoCandidato(id, candidato.getId());
        experienciaRepository.delete(experiencia);
        reindexar(candidato);
    }

    private Candidatos buscarCandidatoPorUsuario(Long usuarioId) {
        return candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de candidato não encontrado para o usuário ID: " + usuarioId));
    }

    private Experiencia buscarExperienciaDoCandidato(Long id, Long candidatoId) {
        Experiencia experiencia = experienciaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Experiência não encontrada com ID: " + id));
        if (!experiencia.getCandidato().getId().equals(candidatoId)) {
            throw new ResourceNotFoundException("Experiência não pertence ao candidato autenticado.");
        }
        return experiencia;
    }

    private ExperienciaResponseDTO toResponseDTO(Experiencia e) {
        return new ExperienciaResponseDTO(
                e.getId(),
                e.getCandidato().getId(),
                e.getEmpresa(),
                e.getCargo(),
                e.getDescricao(),
                e.getDataInicio(),
                e.getDataFim()
        );
    }

    private void reindexar(Candidatos candidato) {
        aposCommitExecutor.executar(() -> indexacaoEmbeddingService.indexarCandidato(candidato));
    }
}
