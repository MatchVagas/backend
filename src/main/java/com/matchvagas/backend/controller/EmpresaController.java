package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.EmpresaResponseDTO;
import com.matchvagas.backend.dto.EmpresaRequestDTO;
import com.matchvagas.backend.service.EmpresaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
@Tag(name = "Empresas", description = "Gerenciamento de perfil de empresas")
public class EmpresaController {

    private final EmpresaService empresaService;

    @GetMapping("/{id}")
    @Operation(summary = "Buscar empresa por ID")
    public ResponseEntity<EmpresaResponseDTO> findById(@PathVariable Long id) {
        EmpresaResponseDTO response = empresaService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPRESA')") // Protege o endpoint para que apenas empresas possam acessar
    @Operation(summary = "Atualizar perfil da empresa")
    public ResponseEntity<EmpresaResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody EmpresaRequestDTO dto) {
        EmpresaResponseDTO response = empresaService.update(id, dto);
        return ResponseEntity.ok(response);
    }
}