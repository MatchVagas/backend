package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.VagaRequestDTO;
import com.matchvagas.backend.dto.VagaResponseDTO;
import com.matchvagas.backend.entity.*;
import com.matchvagas.backend.entity.Usuarios.TipoUsuario;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.VagasMapper;
import com.matchvagas.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VagaService {

    private final VagaRepository vagaRepository;
    private final EmpresaRepository empresaRepository;
    private final TipoVagaRepository tipoVagaRepository;
    private final ModalidadeRepository modalidadeRepository;
    private final EscolaridadeRepository escolaridadeRepository;
    private final StatusVagaRepository statusVagaRepository;
    private final CidadeRepository cidadeRepository;
    private final VagasMapper vagasMapper;

    @Transactional(readOnly = true)
    public List<VagaResponseDTO> findAll() {
        return vagaRepository.findAll()
                .stream().map(vagasMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VagaResponseDTO findById(Long id) {
        Vagas vaga = vagaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaga não encontrada com ID: " + id));
        return vagasMapper.toDTO(vaga);
    }

    @Transactional(readOnly = true)
    public List<VagaResponseDTO> findByEmpresa(Long empresaId) {
        return vagaRepository.findByEmpresasId(empresaId)
                .stream().map(vagasMapper::toDTO).collect(Collectors.toList());
    }

    // Listar vagas da empresa do usuário autenticado
    @Transactional(readOnly = true)
    public List<VagaResponseDTO> findMinhasVagas() {
        Long usuarioId = getUsuarioIdAutenticado();
        Empresas empresa = empresaRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new BusinessException("Nenhuma empresa vinculada a este usuário."));
        return vagaRepository.findByEmpresasId(empresa.getId())
                .stream().map(vagasMapper::toDTO).collect(Collectors.toList());
    }

    // RF007 — Busca e filtragem de vagas (todos os filtros combinados)
    @Transactional(readOnly = true)
    public List<VagaResponseDTO> search(String titulo, String areaAtuacao,
                                        Long tipoVagaId, Long modalidadeId, String nomeEmpresa) {
        String t = blankToNull(titulo);
        String a = blankToNull(areaAtuacao);
        String e = blankToNull(nomeEmpresa);
        return vagaRepository.searchComFiltros(t, a, tipoVagaId, modalidadeId, e)
                .stream().map(vagasMapper::toDTO).collect(Collectors.toList());
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    // RF005 — Cadastrar vaga
    @Transactional
    public VagaResponseDTO create(VagaRequestDTO dto) {
        Vagas vaga = vagasMapper.toEntity(dto);

        // Busca a empresa pelo usuário autenticado — garante que a vaga pertence à empresa correta
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(TipoUsuario.ADMIN.name()));

        Empresas empresa;
        if (isAdmin && dto.empresaId() != null) {
            // ADMIN pode criar vaga para qualquer empresa informando o ID
            empresa = empresaRepository.findById(dto.empresaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));
        } else {
            // EMPRESA — busca automaticamente pelo usuário autenticado
            Long usuarioId = getUsuarioIdAutenticado();
            empresa = empresaRepository.findByUsuarioId(usuarioId)
                    .orElseThrow(() -> new BusinessException(
                            "Nenhuma empresa vinculada a este usuário. Cadastre sua empresa primeiro."));
        }

        if (empresa.getStatus() != Empresas.StatusEmpresa.APROVADA) {
            throw new BusinessException(
                    "Sua empresa ainda não foi aprovada. Aguarde a aprovação do administrador.");
        }

        vaga.setEmpresas(empresa);
        vaga.setTipoVaga(tipoVagaRepository.findById(dto.tipoVagaId())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de vaga não encontrado")));
        vaga.setModalidade(modalidadeRepository.findById(dto.modalidadeId())
                .orElseThrow(() -> new ResourceNotFoundException("Modalidade não encontrada")));
        vaga.setEscolaridade(escolaridadeRepository.findById(dto.escolaridadeId())
                .orElseThrow(() -> new ResourceNotFoundException("Nível de escolaridade não encontrado")));
        vaga.setStatus(statusVagaRepository.findById(dto.statusVagaId())
                .orElseThrow(() -> new ResourceNotFoundException("Status de vaga não encontrado")));
        vaga.setCidade(cidadeRepository.findById(dto.cidadeId())
                .orElseThrow(() -> new ResourceNotFoundException("Cidade não encontrada")));

        if (dto.salarioMinimo() != null && dto.salarioMaximo() != null
                && dto.salarioMinimo().compareTo(dto.salarioMaximo()) > 0)
            throw new BusinessException("Salário mínimo não pode ser maior que o salário máximo.");

        return vagasMapper.toDTO(vagaRepository.save(vaga));
    }

    // RF006 — Atualizar vaga
    @Transactional
    public VagaResponseDTO update(Long id, VagaRequestDTO dto) {
        Vagas vaga = vagaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaga não encontrada com ID: " + id));

        // Verifica se a vaga pertence à empresa do usuário autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(TipoUsuario.ADMIN.name()));

        if (!isAdmin) {
            Long usuarioId = getUsuarioIdAutenticado();
            Empresas empresa = empresaRepository.findByUsuarioId(usuarioId)
                    .orElseThrow(() -> new BusinessException("Nenhuma empresa vinculada a este usuário."));
            if (!vaga.getEmpresas().getId().equals(empresa.getId()))
                throw new BusinessException("Você não tem permissão para editar esta vaga.");
        }

        vaga.setTitulo(dto.titulo());
        vaga.setDescricao(dto.descricao());
        vaga.setRequisitos(dto.requisitos());
        vaga.setBeneficios(dto.beneficios());
        vaga.setCargaHoraria(dto.cargaHoraria());
        vaga.setIdadeMinima(dto.idadeMinima());
        vaga.setIdadeMaxima(dto.idadeMaxima());
        vaga.setAreaAtuacao(dto.areaAtuacao());
        vaga.setDataExpiracao(dto.dataExpiracao());
        vaga.setNumeroVagas(dto.numeroVagas());
        vaga.setSalarioMinimo(dto.salarioMinimo());
        vaga.setSalarioMaximo(dto.salarioMaximo());

        if (dto.tipoVagaId() != null)
            vaga.setTipoVaga(tipoVagaRepository.findById(dto.tipoVagaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tipo de vaga não encontrado")));
        if (dto.modalidadeId() != null)
            vaga.setModalidade(modalidadeRepository.findById(dto.modalidadeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Modalidade não encontrada")));
        if (dto.escolaridadeId() != null)
            vaga.setEscolaridade(escolaridadeRepository.findById(dto.escolaridadeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Escolaridade não encontrada")));
        if (dto.statusVagaId() != null)
            vaga.setStatus(statusVagaRepository.findById(dto.statusVagaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Status não encontrado")));
        if (dto.cidadeId() != null)
            vaga.setCidade(cidadeRepository.findById(dto.cidadeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cidade não encontrada")));

        return vagasMapper.toDTO(vagaRepository.save(vaga));
    }

    // RF006 — Remover vaga
    @Transactional
    public void delete(Long id) {
        Vagas vaga = vagaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaga não encontrada com ID: " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(TipoUsuario.ADMIN.name()));

        if (!isAdmin) {
            Long usuarioId = getUsuarioIdAutenticado();
            Empresas empresa = empresaRepository.findByUsuarioId(usuarioId)
                    .orElseThrow(() -> new BusinessException("Nenhuma empresa vinculada a este usuário."));
            if (!vaga.getEmpresas().getId().equals(empresa.getId()))
                throw new BusinessException("Você não tem permissão para remover esta vaga.");
        }

        vagaRepository.deleteById(id);
    }

    private Long getUsuarioIdAutenticado() {
        return Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
