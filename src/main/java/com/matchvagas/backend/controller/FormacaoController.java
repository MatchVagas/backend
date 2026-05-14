package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.FormacaoRequestDTO;
import com.matchvagas.backend.dto.FormacaoResponseDTO;
import com.matchvagas.backend.service.FormacaoService;
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
@RequestMapping("/api/candidatos/formacoes")
@RequiredArgsConstructor
@Tag(name = "Formações", description = "Gerenciamento das formações acadêmicas do candidato")
public class FormacaoController {

    private final FormacaoService formacaoService;

    @GetMapping
    @Operation(summary = "Listar formações do candidato autenticado")
    public ResponseEntity<List<FormacaoResponseDTO>> listar(Authentication authentication) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(formacaoService.listar(usuarioId));
    }

    @PostMapping
    @Operation(summary = "Adicionar formação acadêmica")
    public ResponseEntity<FormacaoResponseDTO> adicionar(
            Authentication authentication,
            @Valid @RequestBody FormacaoRequestDTO dto) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(formacaoService.adicionar(usuarioId, dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar formação acadêmica por ID")
    public ResponseEntity<FormacaoResponseDTO> atualizar(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody FormacaoRequestDTO dto) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(formacaoService.atualizar(usuarioId, id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover formação acadêmica por ID")
    public ResponseEntity<Void> remover(
            Authentication authentication,
            @PathVariable Long id) {
        Long usuarioId = Long.parseLong(authentication.getName());
        formacaoService.remover(usuarioId, id);
        return ResponseEntity.noContent().build();
    }
}
