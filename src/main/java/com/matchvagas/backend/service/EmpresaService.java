package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.EmpresaRequestDTO;
import com.matchvagas.backend.dto.EmpresaResponseDTO;
import com.matchvagas.backend.entity.Empresas;
import com.matchvagas.backend.entity.Porte;
import com.matchvagas.backend.entity.RamoAtuacao;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.mapper.EmpresaMapper;
import com.matchvagas.backend.repository.EmpresaRepository;
import com.matchvagas.backend.repository.PorteRepository;
import com.matchvagas.backend.repository.RamoAtuacaoRepository;
import com.matchvagas.backend.repository.VagaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final EmpresaMapper empresaMapper;
    private final PorteRepository porteRepository;
    private final RamoAtuacaoRepository ramoAtuacaoRepository;
    private final VagaRepository vagaRepository; // Para calcular totalVagasAtivas

    @Transactional(readOnly = true)
    public EmpresaResponseDTO findById(Long id) {
        Empresas empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Empresa não encontrada com ID: " + id));
        return empresaMapper.toResponseDTO(empresa);
    }

    @Transactional
    public EmpresaResponseDTO update(Long id, EmpresaRequestDTO dto) {
        Empresas empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Empresa não encontrada com ID: " + id));

        // Atualiza campos simples
        empresa.setCnpj(dto.cnpj());
        empresa.setRazaoSocial(dto.razaoSocial());
        empresa.setNomeFantasia(dto.nomeFantasia());
        empresa.setDescricao(dto.descricao());
        empresa.setSite(dto.site());

        // Busca e define as entidades relacionadas
        Porte porte = porteRepository.findById(dto.porteId())
                .orElseThrow(() -> new BusinessException("Porte não encontrado"));
        empresa.setPorte(porte);

        RamoAtuacao ramo = ramoAtuacaoRepository.findById(dto.ramoId())
                .orElseThrow(() -> new BusinessException("Ramo de atuação não encontrado"));
        empresa.setRamoAtuacao(ramo);

        // A lógica para atualizar telefones deve ser implementada
        // Exemplo: limpar a lista e adicionar os novos telefones do DTO

        Empresas savedEmpresa = empresaRepository.save(empresa);
        return empresaMapper.toResponseDTO(savedEmpresa);
    }

    // Método auxiliar para calcular o total de vagas ativas (pode ser usado no mapper)
    // public Integer getTotalVagasAtivas(Long empresaId) { ... }
}