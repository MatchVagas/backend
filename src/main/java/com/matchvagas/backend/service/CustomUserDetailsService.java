package com.matchvagas.backend.service;

import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.repository.UsuariosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuariosRepository  usuariosRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuarios usuario = usuariosRepository.findByEmail(email)
                .orElseThrow(()-> new UsernameNotFoundException("Usuário não encontrado: " + email));

        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getSenha())
                .authorities("ROLE_USER") // TODO: melhorar com roles reais (CANDIDATO, EMPRESA, ADMIN)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!Boolean.TRUE.equals(usuario.getAtivo()))
                .build();
    }

}
