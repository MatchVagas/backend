package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.UsuarioResponseDTO;
import com.matchvagas.backend.dto.UsuariosRequestDTO;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.mapper.UsuarioMapper;
import com.matchvagas.backend.repository.TelefoneRepository;
import com.matchvagas.backend.repository.UsuariosRepository;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    @Autowired
    private UsuariosRepository repository;

    @Autowired
    private TelefoneRepository telefoneRepository;

    @Autowired
    private UsuarioMapper mapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> findAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO findById(int id) {
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
        Optional<Usuarios> existingUser = repository.findByEmail(dto.email());
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("Email já cadastrado: " + dto.email());
        }

        Usuarios entity = mapper.toEntity(dto);
        entity.setSenha(passwordEncoder.encode(dto.senha()));

        // Calcular idade se dataNascimento fornecida
        if (dto.dataNascimento() != null) {
            entity.setIdade(calcularIdade(dto.dataNascimento()));
        }

        // Associar telefones se IDs fornecidos
        //if (dto.getTelefoneIds() != null && !dto.getTelefoneIds().isEmpty()) {
        //    List<Telefone> telefones = telefoneRepository.findAllById(dto.getTelefoneIds());
        //   entity.setTelefones(telefones);
        //}

        entity = repository.save(entity);
        return mapper.toResponseDTO(entity);
    }

    @Transactional
    public UsuarioResponseDTO update(Long id, UsuariosRequestDTO dto) {
        Usuarios entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));

        // Verifica se o novo email já está em uso por outro usuário
        //if (!entity.getEmail().equals(dto.email()) && repository.findByEmail(dto.email())) {
        //   throw new IllegalArgumentException("Email já cadastrado: " + dto.email());
        //}

        mapper.updateEntityFromDTO(dto, entity);
        
        // Atualiza a senha apenas se fornecida
        if (dto.senha() != null && !dto.senha().isBlank()) {
            entity.setSenha(passwordEncoder.encode(dto.senha()));
        }

        // Recalcula idade se dataNascimento alterada
        if (dto.dataNascimento() != null) {
            entity.setIdade(calcularIdade(dto.dataNascimento()));
        }

        // Atualiza telefones
        //if (dto.getTelefoneIds() != null) {
        //    List<Telefone> telefones = telefoneRepository.findAllById(dto.getTelefoneIds());
        //    entity.setTelefones(telefones);
        //} else {
        //    entity.setTelefones(new ArrayList<>()); // limpa lista se null
        //}
        
        entity = repository.save(entity);
        return mapper.toResponseDTO(entity);
    }

    //@Transactional
    //public void delete(Long id) {
    //    if (!repository.existsById(id)) {
    //        throw new ResourceNotFoundException("Usuário não encontrado com ID: " + id);
    //    }
    //    repository.deleteById(id);
    //}

    @Transactional
    public void registrarAcesso(String email) {
        Usuarios usuario = repository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com email: " + email));
        usuario.setDataUltimoAcesso(LocalDateTime.now());
        repository.save(usuario);
    }

    public boolean authenticate(String email, String senha) {
        Usuarios usuario = repository.findByEmail(email).orElse(null);
        if (usuario == null || !usuario.getAtivo()) {
            return false;
        }
        return passwordEncoder.matches(senha, usuario.getSenha());
    }

    private Integer calcularIdade(LocalDateTime dataNascimento) {
        if (dataNascimento == null) return null;
        return Period.between(dataNascimento.toLocalDate(), LocalDate.now()).getYears();
    }
}