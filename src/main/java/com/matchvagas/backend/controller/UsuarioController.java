package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.UsuariosRequestDTO;
import com.matchvagas.backend.dto.UsuarioResponseDTO;
import com.matchvagas.backend.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuários", description = "Gerenciamento de usuários do sistema")
public class UsuarioController {

    @Autowired
    private UsuarioService service;

    @GetMapping
    @Operation(summary = "Listar todos os usuários")
    public ResponseEntity<List<UsuarioResponseDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }


    @GetMapping("/email/{email}")
    @Operation(summary = "Buscar usuário por email")
    public ResponseEntity<UsuarioResponseDTO> findByEmail(@PathVariable String email) {
        return ResponseEntity.ok(service.findByEmail(email));
    }

    @PostMapping
    @Operation(summary = "Criar novo usuário")
    public ResponseEntity<UsuarioResponseDTO> create(@Valid @RequestBody UsuariosRequestDTO dto) {
        UsuarioResponseDTO created = service.create(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar usuário existente")
    public ResponseEntity<UsuarioResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UsuariosRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }
}