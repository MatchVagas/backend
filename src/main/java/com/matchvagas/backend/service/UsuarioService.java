package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.UsuarioResponseDTO;
import com.matchvagas.backend.dto.UsuariosRequestDTO;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.UsuarioMapper;
import com.matchvagas.backend.repository.UsuariosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuariosRepository repository;
    private final UsuarioMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> findAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // CORRIGIDO: Long em vez de int
    @Transactional(readOnly = true)
    public UsuarioResponseDTO findById(Long id) {
        Usuarios entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));
        return mapper.toResponseDTO(entity);
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO findByEmail(String email) {
        Usuarios entity = repository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com email: " + email));
        return mapper.toResponseDTO(entity);
    }

    @Transactional
    public UsuarioResponseDTO create(UsuariosRequestDTO dto) {
        if (!Boolean.TRUE.equals(dto.aceitouTermos())) {
            throw new BusinessException("É necessário aceitar os termos de uso e a política de privacidade.");
        }

        if (repository.existsByEmail(dto.email())) {
            throw new BusinessException("Email já cadastrado: " + dto.email());
        }

        Usuarios entity = mapper.toEntity(dto);
        entity.setSenha(passwordEncoder.encode(dto.senha()));
        entity.registrarConsentimento();

        if (dto.dataNascimento() != null) {
            entity.setIdade(calcularIdade(dto.dataNascimento()));
        }

        return mapper.toResponseDTO(repository.save(entity));
    }

    @Transactional
    public UsuarioResponseDTO update(Long id, UsuariosRequestDTO dto) {
        Usuarios entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));

        // CORRIGIDO: verifica email duplicado apenas se for diferente do atual
        if (!entity.getEmail().equals(dto.email()) && repository.existsByEmail(dto.email())) {
            throw new BusinessException("Email já está em uso por outro usuário: " + dto.email());
        }

        mapper.updateEntityFromDTO(dto, entity);

        if (dto.senha() != null && !dto.senha().isBlank()) {
            entity.setSenha(passwordEncoder.encode(dto.senha()));
        }

        if (dto.dataNascimento() != null) {
            entity.setIdade(calcularIdade(dto.dataNascimento()));
        }

        return mapper.toResponseDTO(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Usuário não encontrado com ID: " + id);
        }
        repository.deleteById(id);
    }

    @Transactional
    public void registrarAcesso(String email) {
        Usuarios usuario = repository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com email: " + email));
        usuario.setDataUltimoAcesso(LocalDateTime.now());
        repository.save(usuario);
    }

    private Integer calcularIdade(LocalDateTime dataNascimento) {
        if (dataNascimento == null) return null;
        return Period.between(dataNascimento.toLocalDate(), LocalDate.now()).getYears();
    }
}
