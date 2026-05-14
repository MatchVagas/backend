package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.ExperienciaRequestDTO;
import com.matchvagas.backend.dto.ExperienciaResponseDTO;
import com.matchvagas.backend.service.ExperienciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/candidatos/experiencias")
@RequiredArgsConstructor
@Tag(name = "Experiências", description = "Gerenciamento das experiências profissionais do candidato")
public class ExperienciaController {

    private final ExperienciaService experienciaService;

    @GetMapping
    @Operation(summary = "Listar experiências do candidato autenticado")
    public ResponseEntity<List<ExperienciaResponseDTO>> listar(Authentication authentication) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(experienciaService.listar(usuarioId));
    }

    @PostMapping
    @Operation(summary = "Adicionar experiência profissional")
    public ResponseEntity<ExperienciaResponseDTO> adicionar(
            Authentication authentication,
            @Valid @RequestBody ExperienciaRequestDTO dto) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(experienciaService.adicionar(usuarioId, dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar experiência profissional por ID")
    public ResponseEntity<ExperienciaResponseDTO> atualizar(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody ExperienciaRequestDTO dto) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(experienciaService.atualizar(usuarioId, id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover experiência profissional por ID")
    public ResponseEntity<Void> remover(
            Authentication authentication,
            @PathVariable Long id) {
        Long usuarioId = Long.parseLong(authentication.getName());
        experienciaService.remover(usuarioId, id);
        return ResponseEntity.noContent().build();
    }
}
