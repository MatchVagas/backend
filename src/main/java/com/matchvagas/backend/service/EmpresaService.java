package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.EmpresaRequestDTO;
import com.matchvagas.backend.dto.EmpresaResponseDTO;
import com.matchvagas.backend.entity.Empresas;
import com.matchvagas.backend.entity.Porte;
import com.matchvagas.backend.entity.RamoAtuacao;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.EmpresaMapper;
import com.matchvagas.backend.repository.EmpresaRepository;
import com.matchvagas.backend.repository.PorteRepository;
import com.matchvagas.backend.repository.RamoAtuacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final PorteRepository porteRepository;
    private final RamoAtuacaoRepository ramoAtuacaoRepository;
    private final EmpresaMapper empresaMapper;

    @Transactional(readOnly = true)
    public List<EmpresaResponseDTO> findAll() {
        return empresaRepository.findAll()
                .stream()
                .map(empresaMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmpresaResponseDTO findById(Long id) {
        Empresas empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada com ID: " + id));
        return empresaMapper.toResponseDTO(empresa);
    }

    // RF004 — Criar perfil de empresa
    @Transactional
    public EmpresaResponseDTO create(EmpresaRequestDTO dto) {
        if (empresaRepository.findByCnpjContainingIgnoreCase(dto.cnpj()).isPresent()) {
            throw new BusinessException("Já existe uma empresa cadastrada com este CNPJ.");
        }

        Empresas empresa = empresaMapper.toEntity(dto);

        empresa.setPorte(porteRepository.findById(dto.porteId())
                .orElseThrow(() -> new ResourceNotFoundException("Porte não encontrado")));
        empresa.setRamoAtuacao(ramoAtuacaoRepository.findById(dto.ramoId())
                .orElseThrow(() -> new ResourceNotFoundException("Ramo de atuação não encontrado")));

        return empresaMapper.toResponseDTO(empresaRepository.save(empresa));
    }

    // RF004 — Atualizar perfil de empresa
    @Transactional
    public EmpresaResponseDTO update(Long id, EmpresaRequestDTO dto) {
        Empresas empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada com ID: " + id));

        empresa.setRazaoSocial(dto.razaoSocial());
        empresa.setNomeFantasia(dto.nomeFantasia());
        empresa.setDescricao(dto.descricao());
        empresa.setSite(dto.site());

        if (dto.porteId() != null) {
            empresa.setPorte(porteRepository.findById(dto.porteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Porte não encontrado")));
        }
        if (dto.ramoId() != null) {
            empresa.setRamoAtuacao(ramoAtuacaoRepository.findById(dto.ramoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ramo de atuação não encontrado")));
        }

        return empresaMapper.toResponseDTO(empresaRepository.save(empresa));
    }
}
