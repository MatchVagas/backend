package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.*;
import com.matchvagas.backend.dto.AuthResponse;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.UsuarioMapper;
import com.matchvagas.backend.repository.UsuariosRepository;
import com.matchvagas.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
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
     * RF001 - Cadastro de novo usuário (Candidato ou Empresa)
     */
    @Transactional
    public UsuarioResponseDTO register(UsuariosRequestDTO request) {

        // Verifica se email já existe
        if (usuariosRepository.existsByEmail(request.email())) {
            throw new BusinessException("Já existe um usuário cadastrado com este email.");
        }

        // Converte DTO para Entity
        Usuarios usuario = usuariosMapper.toEntity(request);

        // Hash da senha
        usuario.setSenha(passwordEncoder.encode(request.senha()));

        // Salva no banco
        Usuarios savedUsuario = usuariosRepository.save(usuario);

        return usuariosMapper.toDTO(savedUsuario);
    }

    /**
     * RF002 - Realiza login e retorna JWT Token
     */
    public AuthResponse login(LoginRequestDTO request) {

        Usuarios usuario = usuariosRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o email informado."));

        // Verifica senha
        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new BusinessException("Email ou senha inválidos.");
        }

        // Verifica se usuário está ativo
        if (Boolean.FALSE.equals(usuario.getAtivo())) {
            throw new BusinessException("Usuário está inativo. Contate o administrador.");
        }

        // Gera o token JWT
        String token = jwtTokenProvider.generateToken(usuario);

        // Determina o perfil (pode ser melhorado com roles no futuro)
        String perfil = determinarPerfil(usuario);

        return new AuthResponse(
                token,
                "Bearer",
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                perfil
        );
    }

    /**
     * Método auxiliar para determinar o perfil do usuário
     * (Pode ser aprimorado com @ManyToMany de roles ou verificação em tabelas filhas)
     */
    private String determinarPerfil(Usuarios usuario) {
        // Lógica temporária - pode ser melhorada depois
        if (usuario.getId() != null) {
            // Verificar se existe registro em Candidatos ou Empresas
            // Por enquanto retorna genérico
            return "CANDIDATO"; // TODO: implementar detecção real de perfil
        }
        return "USUARIO";
    }
}