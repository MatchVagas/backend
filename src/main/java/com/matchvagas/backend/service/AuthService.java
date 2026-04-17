package com.matchvagas.backend.service;

import com.matchvagas.backend.mapper.UsuarioMapper;
import com.matchvagas.backend.repository.UsuariosRepository;
import com.matchvagas.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuariosRepository usuariosRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider  jwtTokenProvider;


}
