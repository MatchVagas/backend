package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.PageResponseDTO;
import com.matchvagas.backend.dto.VagaResponseDTO;
import com.matchvagas.backend.service.FavoritoVagaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/favoritos")
@RequiredArgsConstructor
@Tag(name = "Favoritos", description = "Vagas salvas pelo candidato para ver depois (Fase 2)")
public class FavoritoController {

    private final FavoritoVagaService favoritoService;

    @PostMapping("/{vagaId}")
    @PreAuthorize("hasAuthority('CANDIDATO')")
    @Operation(summary = "Salvar (favoritar) uma vaga", description = "Idempotente: salvar de novo não duplica.")
    public ResponseEntity<VagaResponseDTO> favoritar(@PathVariable Long vagaId, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(favoritoService.favoritar(usuarioId(auth), vagaId));
    }

    @DeleteMapping("/{vagaId}")
    @PreAuthorize("hasAuthority('CANDIDATO')")
    @Operation(summary = "Remover uma vaga dos favoritos")
    public ResponseEntity<Void> desfavoritar(@PathVariable Long vagaId, Authentication auth) {
        favoritoService.desfavoritar(usuarioId(auth), vagaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CANDIDATO')")
    @Operation(summary = "Listar minhas vagas favoritadas (paginado, mais recentes primeiro)")
    public ResponseEntity<PageResponseDTO<VagaResponseDTO>> listar(
            Authentication auth,
            @PageableDefault(size = 20, sort = "dataFavoritado", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(favoritoService.listar(usuarioId(auth), pageable));
    }

    @GetMapping("/{vagaId}")
    @PreAuthorize("hasAuthority('CANDIDATO')")
    @Operation(summary = "Verificar se uma vaga está nos meus favoritos")
    public ResponseEntity<Map<String, Boolean>> estaFavoritada(@PathVariable Long vagaId, Authentication auth) {
        return ResponseEntity.ok(Map.of("favoritada", favoritoService.estaFavoritada(usuarioId(auth), vagaId)));
    }

    private Long usuarioId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }
}
