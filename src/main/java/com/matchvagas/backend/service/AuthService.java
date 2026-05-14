package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.*;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.mapper.UsuarioMapper;
import com.matchvagas.backend.repository.UsuariosRepository;
import com.matchvagas.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuariosRepository usuariosRepository;
    private final UsuarioMapper usuariosMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * RF001 - Cadastro de novo usuário (Candidato, Empresa ou Admin)
     */
    @Transactional
    public UsuarioResponseDTO register(UsuariosRequestDTO request) {

        if (usuariosRepository.existsByEmail(request.email())) {
            throw new BusinessException("Já existe um usuário cadastrado com este email.");
        }

        Usuarios usuario = usuariosMapper.toEntity(request);
        usuario.setSenha(passwordEncoder.encode(request.senha()));

        // Calcula idade a partir da dataNascimento
        if (request.dataNascimento() != null) {
            int idade = java.time.Period.between(
                request.dataNascimento().toLocalDate(),
                java.time.LocalDate.now()
            ).getYears();
            usuario.setIdade(idade);
        }

        // Usuários EMPRESA ficam inativos até aprovação de um ADMIN
        if (request.tipoUsuario() == Usuarios.TipoUsuario.EMPRESA) {
            usuario.setAtivo(false);
        }

        Usuarios salvo = usuariosRepository.save(usuario);
        return usuariosMapper.toDTO(salvo);
    }

    /**
     * RF002 - Autenticação (Login)
     */
    public AuthResponse login(LoginRequestDTO request) {

        Usuarios usuario = usuariosRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Email ou senha inválidos."));

        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new BadCredentialsException("Email ou senha inválidos.");
        }

        if (Boolean.FALSE.equals(usuario.getAtivo())) {
            throw new BadCredentialsException("Usuário está inativo. Contate o administrador.");
        }

        String token = jwtTokenProvider.generateToken(usuario);

        // Usa o tipoUsuario real — não mais hardcoded
        String perfil = usuario.getTipoUsuario() != null
                ? usuario.getTipoUsuario().name()
                : "CANDIDATO";

        return new AuthResponse(
                token,
                "Bearer",
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                perfil
        );
    }
}
