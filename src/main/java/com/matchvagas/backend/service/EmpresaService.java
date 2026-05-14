package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.EmpresaRequestDTO;
import com.matchvagas.backend.dto.EmpresaResponseDTO;
import com.matchvagas.backend.dto.TelefonesRequestDTO;
import com.matchvagas.backend.entity.Empresas;
import com.matchvagas.backend.entity.Telefones;
import com.matchvagas.backend.entity.TipoTelefone;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.entity.Usuarios.TipoUsuario;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.EmpresaMapper;
import com.matchvagas.backend.repository.EmpresaRepository;
import com.matchvagas.backend.repository.PorteRepository;
import com.matchvagas.backend.repository.RamoAtuacaoRepository;
import com.matchvagas.backend.repository.TelefoneRepository;
import com.matchvagas.backend.repository.TipoTelefoneRepository;
import com.matchvagas.backend.repository.UsuariosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final PorteRepository porteRepository;
    private final RamoAtuacaoRepository ramoAtuacaoRepository;
    private final UsuariosRepository usuariosRepository;
    private final TelefoneRepository telefoneRepository;
    private final TipoTelefoneRepository tipoTelefoneRepository;
    private final EmpresaMapper empresaMapper;

    @Transactional(readOnly = true)
    public List<EmpresaResponseDTO> findAll() {
        return empresaRepository.findByStatus(Empresas.StatusEmpresa.APROVADA)
                .stream()
                .map(empresaMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmpresaResponseDTO findById(Long id) {
        Empresas empresa = empresaRepository.findById(id)
                .filter(e -> e.getStatus() == Empresas.StatusEmpresa.APROVADA)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada com ID: " + id));
        return empresaMapper.toResponseDTO(empresa);
    }

    @Transactional(readOnly = true)
    public EmpresaResponseDTO findByUsuarioAutenticado() {
        Long usuarioId = getUsuarioIdAutenticado();
        Empresas empresa = empresaRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhuma empresa vinculada a este usuário."));
        return empresaMapper.toResponseDTO(empresa);
    }

    @Transactional
    public EmpresaResponseDTO create(EmpresaRequestDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(TipoUsuario.ADMIN.name()));
        boolean isEmpresa = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(TipoUsuario.EMPRESA.name()));

        if (!isEmpresa && !isAdmin) {
            throw new BusinessException("Apenas usuários do tipo EMPRESA ou ADMIN podem cadastrar uma empresa.");
        }

        if (empresaRepository.findByCnpjContainingIgnoreCase(dto.cnpj()).isPresent()) {
            throw new BusinessException("Já existe uma empresa cadastrada com este CNPJ.");
        }

        Long usuarioId;
        if (isEmpresa) {
            usuarioId = getUsuarioIdAutenticado();
            if (empresaRepository.findByUsuarioId(usuarioId).isPresent()) {
                throw new BusinessException("Este usuário já possui uma empresa cadastrada.");
            }
        } else {
            usuarioId = dto.usuarioGestorId() != null ? dto.usuarioGestorId() : getUsuarioIdAutenticado();
        }

        Usuarios usuarioGestor = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário gestor não encontrado"));

        Empresas empresa = empresaMapper.toEntity(dto);
        empresa.setUsuario(usuarioGestor);
        empresa.setPorte(porteRepository.findById(dto.porteId())
                .orElseThrow(() -> new ResourceNotFoundException("Porte não encontrado")));
        empresa.setRamoAtuacao(ramoAtuacaoRepository.findById(dto.ramoId())
                .orElseThrow(() -> new ResourceNotFoundException("Ramo de atuação não encontrado")));
        // Admin cria empresa já aprovada; EMPRESA user fica pendente para revisão
        empresa.setStatus(isAdmin ? Empresas.StatusEmpresa.APROVADA : Empresas.StatusEmpresa.PENDENTE);

        Empresas salva = empresaRepository.save(empresa);

        if (dto.telefone() != null) {
            vincularTelefone(dto.telefone(), salva);
            empresaRepository.save(salva);
        }

        return empresaMapper.toResponseDTO(salva);
    }

    @Transactional
    public EmpresaResponseDTO update(Long id, EmpresaRequestDTO dto) {
        Empresas empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada com ID: " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(TipoUsuario.ADMIN.name()));

        if (!isAdmin) {
            Long usuarioId = getUsuarioIdAutenticado();
            if (empresa.getUsuario() == null || !empresa.getUsuario().getId().equals(usuarioId)) {
                throw new BusinessException("Você não tem permissão para editar esta empresa.");
            }
        }

        empresa.setRazaoSocial(dto.razaoSocial());
        empresa.setNomeFantasia(dto.nomeFantasia());
        empresa.setDescricao(dto.descricao());
        empresa.setSite(dto.site());

        if (dto.porteId() != null)
            empresa.setPorte(porteRepository.findById(dto.porteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Porte não encontrado")));
        if (dto.ramoId() != null)
            empresa.setRamoAtuacao(ramoAtuacaoRepository.findById(dto.ramoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ramo de atuação não encontrado")));

        if (dto.telefone() != null) {
            vincularTelefone(dto.telefone(), empresa);
        }

        return empresaMapper.toResponseDTO(empresaRepository.save(empresa));
    }

    private void vincularTelefone(TelefonesRequestDTO dto, Empresas empresa) {
        TipoTelefone tipo = tipoTelefoneRepository.findById(dto.tipoTelefoneId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tipo de telefone não encontrado: " + dto.tipoTelefoneId()));

        Telefones telefone = telefoneRepository.findByNumero(dto.numero())
                .orElseGet(() -> {
                    Telefones novo = new Telefones();
                    novo.setNumero(dto.numero());
                    novo.setTipoTelefone(tipo);
                    novo.setWpp(dto.wpp());
                    return telefoneRepository.save(novo);
                });

        if (empresa.getTelefones() == null) {
            empresa.setTelefones(new ArrayList<>());
        }
        if (!empresa.getTelefones().contains(telefone)) {
            empresa.getTelefones().add(telefone);
        }
    }

    private Long getUsuarioIdAutenticado() {
        return Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
