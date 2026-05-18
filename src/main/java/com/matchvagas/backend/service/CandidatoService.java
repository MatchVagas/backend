package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.CandidatoRequestDTO;
import com.matchvagas.backend.dto.CandidatoResponseDTO;
import com.matchvagas.backend.dto.LocalizacaoRequestDTO;
import com.matchvagas.backend.dto.TelefonesRequestDTO;
import java.time.LocalDate;
import java.time.Period;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Endereco;
import com.matchvagas.backend.entity.Telefones;
import com.matchvagas.backend.entity.TipoTelefone;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.CandidatoMapper;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.TelefoneRepository;
import com.matchvagas.backend.repository.TipoTelefoneRepository;
import com.matchvagas.backend.repository.UsuariosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class CandidatoService {

    private final CandidatoRepository candidatoRepository;
    private final UsuariosRepository usuariosRepository;
    private final CandidatoMapper candidatoMapper;
    private final TelefoneRepository telefoneRepository;
    private final TipoTelefoneRepository tipoTelefoneRepository;

    // RF003 — Buscar perfil do candidato pelo ID do usuário autenticado
    @Transactional(readOnly = true)
    public CandidatoResponseDTO findByUsuarioId(Long usuarioId) {
        Candidatos candidato = candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de candidato não encontrado para o usuário ID: " + usuarioId));
        return candidatoMapper.toResponseDTO(candidato);
    }

    @Transactional(readOnly = true)
    public CandidatoResponseDTO findById(Long id) {
        Candidatos candidato = candidatoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidato não encontrado com ID: " + id));
        return candidatoMapper.toResponseDTO(candidato);
    }

    // RF003 — Criar perfil de candidato vinculado ao usuário autenticado
    @Transactional
    public CandidatoResponseDTO create(Long usuarioId, CandidatoRequestDTO dto) {
        if (candidatoRepository.findByUsuarioId(usuarioId).isPresent()) {
            throw new BusinessException("Já existe um perfil de candidato para este usuário.");
        }

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + usuarioId));

        atualizarDadosPessoais(dto, usuario);

        Candidatos candidato = candidatoMapper.toEntity(dto);
        candidato.setUsuario(usuario);

        if (dto.localizacao() != null) {
            candidato.setEndereco(toEndereco(dto.localizacao()));
        }

        if (dto.telefone() != null) {
            vincularTelefone(dto.telefone(), usuario);
        }

        return candidatoMapper.toResponseDTO(candidatoRepository.save(candidato));
    }

    // RF003 — Atualizar perfil do candidato
    @Transactional
    public CandidatoResponseDTO update(Long usuarioId, CandidatoRequestDTO dto) {
        Candidatos candidato = candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de candidato não encontrado para o usuário ID: " + usuarioId));

        atualizarDadosPessoais(dto, candidato.getUsuario());

        candidato.setCpf(dto.cpf() != null ? dto.cpf() : candidato.getCpf());
        candidato.setObjetivoProfissional(dto.resumoProfissional());
        candidato.setDisponibilidade(dto.disponibilidade());
        candidato.setPretensaoSalarial(dto.pretensaoSalarial());
        if (dto.genero() != null) candidato.setGenero(dto.genero());

        if (dto.localizacao() != null) {
            if (candidato.getEndereco() == null) {
                candidato.setEndereco(toEndereco(dto.localizacao()));
            } else {
                atualizarEndereco(candidato.getEndereco(), dto.localizacao());
            }
        }

        if (dto.telefone() != null) {
            vincularTelefone(dto.telefone(), candidato.getUsuario());
        }

        return candidatoMapper.toResponseDTO(candidatoRepository.save(candidato));
    }

    private void atualizarDadosPessoais(CandidatoRequestDTO dto, Usuarios usuario) {
        if (dto.nomeCompleto() != null && !dto.nomeCompleto().isBlank()) {
            usuario.setNome(dto.nomeCompleto());
        }
        if (dto.email() != null && !dto.email().isBlank() && !dto.email().equalsIgnoreCase(usuario.getEmail())) {
            if (usuariosRepository.existsByEmail(dto.email())) {
                throw new BusinessException("Já existe um usuário cadastrado com este email.");
            }
            usuario.setEmail(dto.email());
        }
        if (dto.dataNascimento() != null) {
            usuario.setDataNascimento(java.sql.Date.valueOf(dto.dataNascimento()));
            usuario.setIdade(Period.between(dto.dataNascimento(), LocalDate.now()).getYears());
        }
    }

    private Endereco toEndereco(LocalizacaoRequestDTO loc) {
        return new Endereco(
                loc.logradouro(),
                loc.numero(),
                loc.complemento() != null ? loc.complemento() : "",
                loc.estado(),
                loc.cidade(),
                loc.bairro(),
                loc.cep()
        );
    }

    private void atualizarEndereco(Endereco endereco, LocalizacaoRequestDTO loc) {
        endereco.setLogradouro(loc.logradouro());
        endereco.setNumero(loc.numero());
        endereco.setCompleto(loc.complemento() != null ? loc.complemento() : "");
        endereco.setEstado(loc.estado());
        endereco.setCidade(loc.cidade());
        endereco.setBairro(loc.bairro());
        endereco.setCep(loc.cep());
    }

    private void vincularTelefone(TelefonesRequestDTO dto, Usuarios usuario) {
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

        if (usuario.getTelefones() == null) {
            usuario.setTelefones(new ArrayList<>());
        }
        if (!usuario.getTelefones().contains(telefone)) {
            usuario.getTelefones().add(telefone);
            usuariosRepository.save(usuario);
        }
    }
}
