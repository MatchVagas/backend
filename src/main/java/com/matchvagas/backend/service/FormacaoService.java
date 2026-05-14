package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.FormacaoRequestDTO;
import com.matchvagas.backend.dto.FormacaoResponseDTO;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Formacao;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.FormacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FormacaoService {

    private final FormacaoRepository formacaoRepository;
    private final CandidatoRepository candidatoRepository;

    @Transactional(readOnly = true)
    public List<FormacaoResponseDTO> listar(Long usuarioId) {
        Candidatos candidato = buscarCandidatoPorUsuario(usuarioId);
        return formacaoRepository.findByCandidatoId(candidato.getId())
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional
    public FormacaoResponseDTO adicionar(Long usuarioId, FormacaoRequestDTO dto) {
        Candidatos candidato = buscarCandidatoPorUsuario(usuarioId);
        Formacao formacao = new Formacao();
        formacao.setInstituicao(dto.instituicao());
        formacao.setCurso(dto.curso());
        formacao.setNivel(dto.nivel());
        formacao.setDataInicio(dto.dataInicio());
        formacao.setDataFim(dto.dataFim());
        formacao.setCandidato(candidato);
        return toResponseDTO(formacaoRepository.save(formacao));
    }

    @Transactional
    public FormacaoResponseDTO atualizar(Long usuarioId, Long id, FormacaoRequestDTO dto) {
        Candidatos candidato = buscarCandidatoPorUsuario(usuarioId);
        Formacao formacao = buscarFormacaoDoCandidato(id, candidato.getId());
        formacao.setInstituicao(dto.instituicao());
        formacao.setCurso(dto.curso());
        formacao.setNivel(dto.nivel());
        formacao.setDataInicio(dto.dataInicio());
        formacao.setDataFim(dto.dataFim());
        return toResponseDTO(formacaoRepository.save(formacao));
    }

    @Transactional
    public void remover(Long usuarioId, Long id) {
        Candidatos candidato = buscarCandidatoPorUsuario(usuarioId);
        Formacao formacao = buscarFormacaoDoCandidato(id, candidato.getId());
        formacaoRepository.delete(formacao);
    }

    private Candidatos buscarCandidatoPorUsuario(Long usuarioId) {
        return candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de candidato não encontrado para o usuário ID: " + usuarioId));
    }

    private Formacao buscarFormacaoDoCandidato(Long id, Long candidatoId) {
        Formacao formacao = formacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Formação não encontrada com ID: " + id));
        if (!formacao.getCandidato().getId().equals(candidatoId)) {
            throw new ResourceNotFoundException("Formação não pertence ao candidato autenticado.");
        }
        return formacao;
    }

    private FormacaoResponseDTO toResponseDTO(Formacao f) {
        return new FormacaoResponseDTO(
                f.getId(),
                f.getCandidato().getId(),
                f.getInstituicao(),
                f.getCurso(),
                f.getNivel(),
                f.getDataInicio(),
                f.getDataFim()
        );
    }
}
