package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.UsuarioResponseDTO;
import com.matchvagas.backend.dto.UsuariosRequestDTO;
import com.matchvagas.backend.dto.LoginRequestDTO;
import com.matchvagas.backend.dto.AuthResponse;
import com.matchvagas.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Ajuste conforme o frontend (recomendado configurar melhor em produção)
public class AuthController {

    private final AuthService authService;

    /**
     * RF001 - Cadastro de novo usuário (Candidato ou Empresa)
     */
    @PostMapping("/register")
    public ResponseEntity<UsuarioResponseDTO> register(@Valid @RequestBody UsuariosRequestDTO request) {
        UsuarioResponseDTO usuarioDTO = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioDTO);
    }

    /**
     * RF002 - Autenticação (Login)
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequestDTO request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(authResponse);
    }

    /**
     * Logout (stateless com JWT - não precisa invalidar token no backend) ???
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // Como estamos usando JWT stateless, basta o frontend remover o token ???
        return ResponseEntity.ok().build();
    }
}