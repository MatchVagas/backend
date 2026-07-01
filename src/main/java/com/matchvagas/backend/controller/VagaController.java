package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.PageResponseDTO;
import com.matchvagas.backend.dto.VagaRequestDTO;
import com.matchvagas.backend.dto.VagaResponseDTO;
import com.matchvagas.backend.service.VagaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vagas")
@RequiredArgsConstructor
@Tag(name = "Vagas", description = "Cadastro, manutenção e busca de vagas (RF005, RF006, RF007)")
public class VagaController {

    private final VagaService vagaService;

    // RF007 — Busca pública com filtros opcionais (paginada)
    @GetMapping
    @Operation(summary = "Buscar e filtrar vagas",
               description = "Suporta paginação via ?page=0&size=20&sort=id,desc (padrão: 20 por página).")
    public ResponseEntity<PageResponseDTO<VagaResponseDTO>> search(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String areaAtuacao,
            @RequestParam(required = false) Long tipoVagaId,
            @RequestParam(required = false) Long modalidadeId,
            @RequestParam(required = false) String nomeEmpresa,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(
                vagaService.search(titulo, areaAtuacao, tipoVagaId, modalidadeId, nomeEmpresa, pageable));
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

    // Empresa — listar as próprias vagas
    @GetMapping("/minhas")
    @PreAuthorize("hasAuthority('EMPRESA')")
    @Operation(summary = "Listar minhas vagas")
    public ResponseEntity<List<VagaResponseDTO>> minhasVagas() {
        return ResponseEntity.ok(vagaService.findMinhasVagas());
    }

    // RF005 — Cadastrar vaga (EMPRESA ou ADMIN)
    @PostMapping
    @PreAuthorize("hasAuthority('EMPRESA') or hasAuthority('ADMIN')")
    @Operation(summary = "Cadastrar nova vaga")
    public ResponseEntity<VagaResponseDTO> create(@Valid @RequestBody VagaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vagaService.create(dto));
    }

    // RF006 — Atualizar vaga (EMPRESA ou ADMIN)
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPRESA') or hasAuthority('ADMIN')")
    @Operation(summary = "Atualizar vaga existente")
    public ResponseEntity<VagaResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody VagaRequestDTO dto) {
        return ResponseEntity.ok(vagaService.update(id, dto));
    }

    // RF006 — Remover vaga (EMPRESA ou ADMIN)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPRESA') or hasAuthority('ADMIN')")
    @Operation(summary = "Remover vaga")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vagaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
