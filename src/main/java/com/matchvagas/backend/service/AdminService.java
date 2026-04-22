package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.AdministradorRequestDTO;
import com.matchvagas.backend.dto.AdministradorResponseDTO;
import com.matchvagas.backend.dto.CandidatoResponseDTO;
import com.matchvagas.backend.dto.EmpresaResponseDTO;
import com.matchvagas.backend.dto.UsuarioResponseDTO;
import com.matchvagas.backend.entity.Administradores;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Empresas;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.AdministradoresMapper;
import com.matchvagas.backend.mapper.CandidatoMapper;
import com.matchvagas.backend.mapper.EmpresaMapper;
import com.matchvagas.backend.mapper.UsuarioMapper;
import com.matchvagas.backend.repository.AdministradoresRepository;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.DepartamentosRepository;
import com.matchvagas.backend.repository.EmpresaRepository;
import com.matchvagas.backend.repository.UsuariosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UsuariosRepository usuariosRepository;
    private final CandidatoRepository candidatoRepository;
    private final EmpresaRepository empresaRepository;
    private final AdministradoresRepository administradoresRepository;
    private final DepartamentosRepository departamentosRepository;
    private final UsuarioMapper usuarioMapper;
    private final CandidatoMapper candidatoMapper;
    private final EmpresaMapper empresaMapper;
    private final AdministradoresMapper administradoresMapper;

    // ── Gestão de Usuários ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarUsuarios() {
        return usuariosRepository.findAll()
                .stream()
                .map(usuarioMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void ativarDesativarUsuario(Long usuarioId, boolean ativo) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + usuarioId));
        usuario.setAtivo(ativo);
        usuariosRepository.save(usuario);
    }

    @Transactional
    public void excluirUsuario(Long usuarioId) {
        if (!usuariosRepository.existsById(usuarioId)) {
            throw new ResourceNotFoundException("Usuário não encontrado com ID: " + usuarioId);
        }
        usuariosRepository.deleteById(usuarioId);
    }

    // ── Gestão de Candidatos ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CandidatoResponseDTO> listarCandidatos() {
        return candidatoRepository.findAll()
                .stream()
                .map(candidatoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ── Gestão de Empresas ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EmpresaResponseDTO> listarEmpresas() {
        return empresaRepository.findAll()
                .stream()
                .map(empresaMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ── Gestão de Administradores ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AdministradorResponseDTO> listarAdmins() {
        return administradoresRepository.findAll()
                .stream()
                .map(administradoresMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public AdministradorResponseDTO criarAdmin(AdministradorRequestDTO dto) {
        Usuarios usuario = usuariosRepository.findById(dto.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + dto.usuarioId()));

        // Verifica se já é admin
        if (!administradoresRepository.findByUsuarioId(dto.usuarioId()).isEmpty()) {
            throw new BusinessException("Este usuário já é administrador.");
        }

        Administradores admin = administradoresMapper.toEntity(dto);
        admin.setUsuario(usuario);
        admin.setDepartamento(departamentosRepository.findById(dto.departamentoId())
                .orElseThrow(() -> new ResourceNotFoundException("Departamento não encontrado")));

        // Promove o tipo do usuário para ADMIN
        usuario.setTipoUsuario(Usuarios.TipoUsuario.ADMIN);
        usuariosRepository.save(usuario);

        return administradoresMapper.toResponseDTO(administradoresRepository.save(admin));
    }

    @Transactional
    public void removerAdmin(Long adminId) {
        Administradores admin = administradoresRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Administrador não encontrado com ID: " + adminId));

        // Reverte o tipo do usuário para CANDIDATO
        Usuarios usuario = admin.getUsuario();
        usuario.setTipoUsuario(Usuarios.TipoUsuario.CANDIDATO);
        usuariosRepository.save(usuario);

        administradoresRepository.deleteById(adminId);
    }
}
