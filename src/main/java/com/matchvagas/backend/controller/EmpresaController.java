package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.EmpresaRequestDTO;
import com.matchvagas.backend.dto.EmpresaResponseDTO;
import com.matchvagas.backend.service.EmpresaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
@Tag(name = "Empresas", description = "Gerenciamento de perfil de empresa (RF004)")
public class EmpresaController {

    private final EmpresaService empresaService;

    @GetMapping
    @Operation(summary = "Listar todas as empresas")
    public ResponseEntity<List<EmpresaResponseDTO>> findAll() {
        return ResponseEntity.ok(empresaService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar empresa por ID")
    public ResponseEntity<EmpresaResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(empresaService.findById(id));
    }

    // Usuário EMPRESA visualiza sua própria empresa
    @GetMapping("/minha-empresa")
    @PreAuthorize("hasAuthority('EMPRESA')")
    @Operation(summary = "Visualizar minha empresa")
    public ResponseEntity<EmpresaResponseDTO> minhaEmpresa() {
        return ResponseEntity.ok(empresaService.findByUsuarioAutenticado());
    }

    // RF004 — Criar perfil de empresa (EMPRESA ou ADMIN)
    @PostMapping
    @PreAuthorize("hasAuthority('EMPRESA') or hasAuthority('ADMIN')")
    @Operation(summary = "Cadastrar empresa — restrito a usuários do tipo EMPRESA ou ADMIN")
    public ResponseEntity<EmpresaResponseDTO> create(@Valid @RequestBody EmpresaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(empresaService.create(dto));
    }

    // RF004 — Atualizar perfil de empresa (EMPRESA ou ADMIN)
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPRESA') or hasAuthority('ADMIN')")
    @Operation(summary = "Atualizar empresa — restrito a usuários do tipo EMPRESA ou ADMIN")
    public ResponseEntity<EmpresaResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody EmpresaRequestDTO dto) {
        return ResponseEntity.ok(empresaService.update(id, dto));
    }
}
