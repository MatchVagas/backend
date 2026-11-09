package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.BuscaTalentosRequestDTO;
import com.matchvagas.backend.dto.CandidatoRecomendadoResponseDTO;
import com.matchvagas.backend.dto.EmpresaRequestDTO;
import com.matchvagas.backend.dto.EmpresaResponseDTO;
import com.matchvagas.backend.dto.PageResponseDTO;
import com.matchvagas.backend.service.BuscaTalentosService;
import com.matchvagas.backend.service.EmpresaService;
import com.matchvagas.backend.service.FotoPerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
@Tag(name = "Empresas", description = "Gerenciamento de perfil de empresa (RF004)")
public class EmpresaController {

    private final EmpresaService   empresaService;
    private final FotoPerfilService fotoPerfilService;
    private final BuscaTalentosService buscaTalentosService;

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

    @PostMapping(value = "/minha-empresa/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EMPRESA')")
    @Operation(summary = "Enviar logo da empresa (JPG, PNG ou WebP — máx. 5 MB)")
    public ResponseEntity<String> uploadLogo(
            Authentication authentication,
            @RequestParam("arquivo") MultipartFile arquivo) {
        Long usuarioId = Long.parseLong(authentication.getName());
        String url = fotoPerfilService.uploadEmpresa(usuarioId, arquivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(url);
    }

    @PutMapping(value = "/minha-empresa/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EMPRESA')")
    @Operation(summary = "Atualizar logo da empresa (JPG, PNG ou WebP — máx. 5 MB)")
    public ResponseEntity<String> atualizarLogo(
            Authentication authentication,
            @RequestParam("arquivo") MultipartFile arquivo) {
        Long usuarioId = Long.parseLong(authentication.getName());
        String url = fotoPerfilService.uploadEmpresa(usuarioId, arquivo);
        return ResponseEntity.ok(url);
    }

    @DeleteMapping("/minha-empresa/logo")
    @PreAuthorize("hasAuthority('EMPRESA')")
    @Operation(summary = "Remover logo da empresa")
    public ResponseEntity<Void> deletarLogo(Authentication authentication) {
        Long usuarioId = Long.parseLong(authentication.getName());
        fotoPerfilService.deletarEmpresa(usuarioId);
        return ResponseEntity.noContent().build();
    }

    // Fase 3 — busca de talentos sem vaga cadastrada
    @PostMapping("/talentos/busca")
    @PreAuthorize("hasAuthority('EMPRESA')")
    @Operation(
        summary = "Buscar candidatos a partir de um perfil ideal descrito em texto livre",
        description = "Não exige vaga cadastrada e nada é salvo. Ranqueia só candidatos com opt-in "
                    + "explícito, com a mesma pontuação e os mesmos dados mínimos da recomendação por vaga "
                    + "(nunca CPF, contato, endereço ou arquivo do currículo). Cada página conta na cota "
                    + "por hora da empresa, e os candidatos retornados ficam registrados na auditoria."
    )
    public ResponseEntity<PageResponseDTO<CandidatoRecomendadoResponseDTO>> buscarTalentos(
            Authentication authentication,
            @Valid @RequestBody BuscaTalentosRequestDTO dto,
            @PageableDefault(size = 20) Pageable pageable) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(buscaTalentosService.buscar(usuarioId, dto, pageable));
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
