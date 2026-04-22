package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.VagaRequestDTO;
import com.matchvagas.backend.dto.VagaResponseDTO;
import com.matchvagas.backend.service.VagaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vagas")
@RequiredArgsConstructor
@Tag(name = "Vagas", description = "Cadastro, manutenção e busca de vagas (RF005, RF006, RF007)")
public class VagaController {

    private final VagaService vagaService;

    // RF007 — Listar/buscar vagas com filtros opcionais
    @GetMapping
    @Operation(summary = "Buscar e filtrar vagas")
    public ResponseEntity<List<VagaResponseDTO>> search(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String areaAtuacao,
            @RequestParam(required = false) Long tipoVagaId,
            @RequestParam(required = false) Long modalidadeId) {
        return ResponseEntity.ok(vagaService.search(titulo, areaAtuacao, tipoVagaId, modalidadeId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar vaga por ID")
    public ResponseEntity<VagaResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(vagaService.findById(id));
    }

    @GetMapping("/empresa/{empresaId}")
    @Operation(summary = "Listar vagas de uma empresa")
    public ResponseEntity<List<VagaResponseDTO>> findByEmpresa(@PathVariable Long empresaId) {
        return ResponseEntity.ok(vagaService.findByEmpresa(empresaId));
    }

    // RF005 — Cadastrar vaga
    @PostMapping
    @Operation(summary = "Cadastrar nova vaga")
    public ResponseEntity<VagaResponseDTO> create(@Valid @RequestBody VagaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vagaService.create(dto));
    }

    // RF006 — Atualizar vaga
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar vaga existente")
    public ResponseEntity<VagaResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody VagaRequestDTO dto) {
        return ResponseEntity.ok(vagaService.update(id, dto));
    }

    // RF006 — Remover vaga
    @DeleteMapping("/{id}")
    @Operation(summary = "Remover vaga")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vagaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
