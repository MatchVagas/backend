package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.HabilidadeRequestDTO;
import com.matchvagas.backend.dto.HabilidadeResponseDTO;
import com.matchvagas.backend.service.HabilidadeService;
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
@RequestMapping("/api/candidatos/habilidades")
@RequiredArgsConstructor
@Tag(name = "Habilidades", description = "Gerenciamento das habilidades declaradas do candidato")
public class HabilidadeController {

    private final HabilidadeService habilidadeService;

    @GetMapping
    @Operation(summary = "Listar habilidades do candidato autenticado")
    public ResponseEntity<List<HabilidadeResponseDTO>> listar(Authentication authentication) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(habilidadeService.listar(usuarioId));
    }

    @PostMapping
    @Operation(summary = "Adicionar habilidade ao perfil do candidato")
    public ResponseEntity<HabilidadeResponseDTO> adicionar(
            Authentication authentication,
            @Valid @RequestBody HabilidadeRequestDTO dto) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(habilidadeService.adicionar(usuarioId, dto));
    }

    @PutMapping("/{nome}")
    @Operation(summary = "Atualizar habilidade pelo nome atual")
    public ResponseEntity<HabilidadeResponseDTO> atualizar(
            Authentication authentication,
            @PathVariable String nome,
            @Valid @RequestBody HabilidadeRequestDTO dto) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(habilidadeService.atualizar(usuarioId, nome, dto));
    }

    @DeleteMapping("/{nome}")
    @Operation(summary = "Remover habilidade pelo nome")
    public ResponseEntity<Void> remover(
            Authentication authentication,
            @PathVariable String nome) {
        Long usuarioId = Long.parseLong(authentication.getName());
        habilidadeService.remover(usuarioId, nome);
        return ResponseEntity.noContent().build();
    }
}
