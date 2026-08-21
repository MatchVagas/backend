package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.*;
import com.matchvagas.backend.entity.*;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.*;
import com.matchvagas.backend.repository.*;
import com.matchvagas.backend.util.CpfCrypto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Period;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UsuariosRepository       usuariosRepository;
    private final CandidatoRepository      candidatoRepository;
    private final EmpresaRepository        empresaRepository;
    private final VagaRepository           vagaRepository;
    private final CandidaturaRepository    candidaturaRepository;
    private final MensagemRepository       mensagemRepository;
    private final AdministradoresRepository administradoresRepository;
    private final DepartamentosRepository  departamentosRepository;
    private final StatusVagaRepository     statusVagaRepository;
    private final StatusCandidaturaRepository statusCandidaturaRepository;
    private final UsuarioMapper            usuarioMapper;
    private final CandidatoMapper          candidatoMapper;
    private final EmpresaMapper            empresaMapper;
    private final VagasMapper              vagasMapper;
    private final CandidaturaMapper        candidaturaMapper;
    private final AdministradoresMapper    administradoresMapper;
    private final PasswordEncoder          passwordEncoder;

    // ═══════════════════════════════════════════════════════════
    //  GESTÃO DE USUÁRIOS
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public PageResponseDTO<UsuarioResponseDTO> listarUsuarios(Pageable pageable) {
        return PageResponseDTO.of(
                usuariosRepository.findAll(pageable).map(usuarioMapper::toResponseDTO));
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarUsuario(Long id) {
        return usuarioMapper.toResponseDTO(
                usuariosRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id)));
    }

    @Transactional
    public UsuarioResponseDTO atualizarUsuario(Long id, AdminAtualizarUsuarioRequestDTO dto) {
        Usuarios usuario = usuariosRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));

        if (!usuario.getEmail().equals(dto.email()) && usuariosRepository.existsByEmail(dto.email())) {
            throw new BusinessException("Já existe outro usuário com o e-mail: " + dto.email());
        }

        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setTipoUsuario(dto.tipoUsuario());

        if (dto.dataNascimento() != null) {
            Date dataNasc = Date.from(dto.dataNascimento().atZone(ZoneId.systemDefault()).toInstant());
            usuario.setDataNascimento(dataNasc);
            usuario.setIdade(Period.between(dto.dataNascimento().toLocalDate(), LocalDate.now()).getYears());
        }
        if (dto.ativo() != null) {
            usuario.setAtivo(dto.ativo());
        }
        if (dto.senha() != null && !dto.senha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(dto.senha()));
        }

        return usuarioMapper.toResponseDTO(usuariosRepository.save(usuario));
    }

    @Transactional
    public void ativarDesativarUsuario(Long id, boolean ativo) {
        Usuarios usuario = usuariosRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));
        usuario.setAtivo(ativo);
        usuariosRepository.save(usuario);
    }

    @Transactional
    public void alterarTipoUsuario(Long id, Usuarios.TipoUsuario novoTipo) {
        Usuarios usuario = usuariosRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));
        usuario.setTipoUsuario(novoTipo);
        usuariosRepository.save(usuario);
    }

    @Transactional
    public void excluirUsuario(Long id) {
        if (!usuariosRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuário não encontrado com ID: " + id);
        }
        usuariosRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarUsuariosEmpresaPendentes() {
        return usuariosRepository.findByTipoUsuarioAndAtivo(Usuarios.TipoUsuario.EMPRESA, false)
                .stream().map(usuarioMapper::toResponseDTO).collect(Collectors.toList());
    }

    @Transactional
    public UsuarioResponseDTO aprovarUsuarioEmpresa(Long id) {
        Usuarios usuario = usuariosRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));
        if (usuario.getTipoUsuario() != Usuarios.TipoUsuario.EMPRESA) {
            throw new BusinessException("Este usuário não é do tipo EMPRESA.");
        }
        if (Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new BusinessException("Usuário já está aprovado.");
        }
        usuario.setAtivo(true);
        return usuarioMapper.toResponseDTO(usuariosRepository.save(usuario));
    }

    @Transactional
    public void rejeitarUsuarioEmpresa(Long id) {
        Usuarios usuario = usuariosRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));
        if (usuario.getTipoUsuario() != Usuarios.TipoUsuario.EMPRESA) {
            throw new BusinessException("Este usuário não é do tipo EMPRESA.");
        }
        if (Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new BusinessException("Não é possível rejeitar um usuário já aprovado. Use desativar.");
        }
        usuariosRepository.deleteById(id);
    }

    // ═══════════════════════════════════════════════════════════
    //  GESTÃO DE CANDIDATOS
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public PageResponseDTO<CandidatoResponseDTO> listarCandidatos(Pageable pageable) {
        return PageResponseDTO.of(
                candidatoRepository.findAll(pageable).map(candidatoMapper::toResponseDTO));
    }

    @Transactional(readOnly = true)
    public CandidatoResponseDTO buscarCandidato(Long id) {
        return candidatoMapper.toResponseDTO(
                candidatoRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Candidato não encontrado com ID: " + id)));
    }

    @Transactional
    public CandidatoResponseDTO atualizarCandidato(Long id, CandidatoRequestDTO dto) {
        Candidatos candidato = candidatoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidato não encontrado com ID: " + id));

        if (dto.cpf() != null && !dto.cpf().isBlank()) {
            candidatoRepository.findByCpfHash(CpfCrypto.hash(dto.cpf()))
                    .filter(c -> !c.getId().equals(id))
                    .ifPresent(c -> { throw new BusinessException("CPF já cadastrado para outro candidato."); });
            candidato.setCpf(dto.cpf());
        }
        if (dto.resumoProfissional() != null)  candidato.setObjetivoProfissional(dto.resumoProfissional());
        if (dto.disponibilidade() != null)     candidato.setDisponibilidade(dto.disponibilidade());
        if (dto.pretensaoSalarial() != null)   candidato.setPretensaoSalarial(dto.pretensaoSalarial());

        return candidatoMapper.toResponseDTO(candidatoRepository.save(candidato));
    }

    @Transactional
    public void excluirCandidato(Long id) {
        if (!candidatoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Candidato não encontrado com ID: " + id);
        }
        candidatoRepository.deleteById(id);
    }

    // ═══════════════════════════════════════════════════════════
    //  GESTÃO DE EMPRESAS
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public PageResponseDTO<EmpresaResponseDTO> listarEmpresas(Pageable pageable) {
        return PageResponseDTO.of(
                empresaRepository.findAll(pageable).map(empresaMapper::toResponseDTO));
    }

    @Transactional(readOnly = true)
    public List<EmpresaResponseDTO> listarEmpresasPendentes() {
        return empresaRepository.findByStatus(Empresas.StatusEmpresa.PENDENTE)
                .stream().map(empresaMapper::toResponseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmpresaResponseDTO buscarEmpresa(Long id) {
        return empresaMapper.toResponseDTO(
                empresaRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada com ID: " + id)));
    }

    @Transactional
    public EmpresaResponseDTO aprovarEmpresa(Long id) {
        Empresas empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada com ID: " + id));
        if (empresa.getStatus() == Empresas.StatusEmpresa.APROVADA) {
            throw new BusinessException("Empresa já está aprovada.");
        }
        empresa.setStatus(Empresas.StatusEmpresa.APROVADA);
        return empresaMapper.toResponseDTO(empresaRepository.save(empresa));
    }

    @Transactional
    public EmpresaResponseDTO rejeitarEmpresa(Long id) {
        Empresas empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada com ID: " + id));
        if (empresa.getStatus() == Empresas.StatusEmpresa.REJEITADA) {
            throw new BusinessException("Empresa já está rejeitada.");
        }
        empresa.setStatus(Empresas.StatusEmpresa.REJEITADA);
        return empresaMapper.toResponseDTO(empresaRepository.save(empresa));
    }

    @Transactional
    public void excluirEmpresa(Long id) {
        if (!empresaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Empresa não encontrada com ID: " + id);
        }
        empresaRepository.deleteById(id);
    }

    // ═══════════════════════════════════════════════════════════
    //  GESTÃO DE VAGAS
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public PageResponseDTO<VagaResponseDTO> listarVagas(Pageable pageable) {
        return PageResponseDTO.of(
                vagaRepository.findAll(pageable).map(vagasMapper::toDTO));
    }

    @Transactional(readOnly = true)
    public VagaResponseDTO buscarVaga(Long id) {
        return vagasMapper.toDTO(
                vagaRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Vaga não encontrada com ID: " + id)));
    }

    @Transactional
    public VagaResponseDTO alterarStatusVaga(Long vagaId, Long statusId) {
        Vagas vaga = vagaRepository.findById(vagaId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaga não encontrada com ID: " + vagaId));

        StatusVaga novoStatus = statusVagaRepository.findById(statusId)
                .orElseThrow(() -> new ResourceNotFoundException("Status de vaga não encontrado com ID: " + statusId));

        vaga.setStatus(novoStatus);
        return vagasMapper.toDTO(vagaRepository.save(vaga));
    }

    @Transactional
    public void excluirVaga(Long id) {
        if (!vagaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Vaga não encontrada com ID: " + id);
        }
        vagaRepository.deleteById(id);
    }

    // ═══════════════════════════════════════════════════════════
    //  GESTÃO DE CANDIDATURAS
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public PageResponseDTO<CandidaturaResponseDTO> listarCandidaturas(Pageable pageable) {
        return PageResponseDTO.of(
                candidaturaRepository.findAll(pageable).map(candidaturaMapper::toResponseDTO));
    }

    @Transactional(readOnly = true)
    public CandidaturaResponseDTO buscarCandidatura(Long id) {
        return candidaturaMapper.toResponseDTO(
                candidaturaRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada com ID: " + id)));
    }

    @Transactional
    public CandidaturaResponseDTO alterarStatusCandidatura(Long candidaturaId, Long statusId) {
        Candidatura candidatura = candidaturaRepository.findById(candidaturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada com ID: " + candidaturaId));

        StatusCandidatura novoStatus = statusCandidaturaRepository.findById(statusId)
                .orElseThrow(() -> new ResourceNotFoundException("Status de candidatura não encontrado com ID: " + statusId));

        candidatura.setStatus(novoStatus);
        return candidaturaMapper.toResponseDTO(candidaturaRepository.save(candidatura));
    }

    @Transactional
    public void excluirCandidatura(Long id) {
        if (!candidaturaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Candidatura não encontrada com ID: " + id);
        }
        // Remove as conversas antes da candidatura (FK mensagens → candidatura)
        mensagemRepository.deleteByCandidaturaId(id);
        candidaturaRepository.deleteById(id);
    }

    // ═══════════════════════════════════════════════════════════
    //  GESTÃO DE ADMINISTRADORES
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<AdministradorResponseDTO> listarAdmins() {
        return administradoresRepository.findAll()
                .stream().map(administradoresMapper::toResponseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdministradorResponseDTO buscarAdmin(Long id) {
        return administradoresMapper.toResponseDTO(
                administradoresRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Administrador não encontrado com ID: " + id)));
    }

    @Transactional
    public AdministradorResponseDTO criarAdmin(AdministradorRequestDTO dto) {
        Usuarios usuario = usuariosRepository.findById(dto.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + dto.usuarioId()));

        if (!administradoresRepository.findByUsuarioId(dto.usuarioId()).isEmpty()) {
            throw new BusinessException("Este usuário já é administrador.");
        }

        Departamentos departamento = departamentosRepository.findById(dto.departamentoId())
                .orElseThrow(() -> new ResourceNotFoundException("Departamento não encontrado com ID: " + dto.departamentoId()));

        Administradores admin = administradoresMapper.toEntity(dto);
        admin.setUsuario(usuario);
        admin.setDepartamento(departamento);

        usuario.setTipoUsuario(Usuarios.TipoUsuario.ADMIN);
        usuariosRepository.save(usuario);

        return administradoresMapper.toResponseDTO(administradoresRepository.save(admin));
    }

    @Transactional
    public AdministradorResponseDTO atualizarAdmin(Long id, AdministradorRequestDTO dto) {
        Administradores admin = administradoresRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administrador não encontrado com ID: " + id));

        Departamentos departamento = departamentosRepository.findById(dto.departamentoId())
                .orElseThrow(() -> new ResourceNotFoundException("Departamento não encontrado com ID: " + dto.departamentoId()));

        admin.setNivel(dto.nivel());
        admin.setPermissoes(dto.permissoes());
        admin.setDepartamento(departamento);

        return administradoresMapper.toResponseDTO(administradoresRepository.save(admin));
    }

    @Transactional
    public void removerAdmin(Long id) {
        Administradores admin = administradoresRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administrador não encontrado com ID: " + id));

        Usuarios usuario = admin.getUsuario();
        usuario.setTipoUsuario(Usuarios.TipoUsuario.CANDIDATO);
        usuariosRepository.save(usuario);

        administradoresRepository.deleteById(id);
    }

    // ═══════════════════════════════════════════════════════════
    //  ESTATÍSTICAS DO SISTEMA
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public EstatisticasSistemaDTO estatisticas() {
        long totalUsuarios    = usuariosRepository.count();
        long usuariosAtivos   = usuariosRepository.findByAtivo(true).size();
        long usuariosInativos = totalUsuarios - usuariosAtivos;
        long totalCandidatos  = candidatoRepository.count();
        long totalEmpresas    = empresaRepository.count();
        long totalVagas       = vagaRepository.count();
        long totalCandidaturas = candidaturaRepository.count();
        long totalAdmins      = administradoresRepository.count();

        return new EstatisticasSistemaDTO(
                totalUsuarios,
                usuariosAtivos,
                usuariosInativos,
                totalCandidatos,
                totalEmpresas,
                totalVagas,
                totalCandidaturas,
                totalAdmins
        );
    }
}
