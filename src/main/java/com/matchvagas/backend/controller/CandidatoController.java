package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.CandidatoRequestDTO;
import com.matchvagas.backend.dto.CandidatoResponseDTO;
import com.matchvagas.backend.dto.SugestaoVagaResponseDTO;
import com.matchvagas.backend.service.CandidatoService;
import com.matchvagas.backend.service.FotoPerfilService;
import com.matchvagas.backend.service.SugestaoVagaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/candidatos")
@RequiredArgsConstructor
@Tag(name = "Candidatos", description = "Gerenciamento de perfil do candidato (RF003)")
public class CandidatoController {

    private final CandidatoService    candidatoService;
    private final SugestaoVagaService sugestaoVagaService;
    private final FotoPerfilService   fotoPerfilService;

    // RF003 — Visualizar próprio perfil
    @GetMapping("/meu-perfil")
    @Operation(summary = "Visualizar meu perfil de candidato")
    public ResponseEntity<CandidatoResponseDTO> meuPerfil(Authentication authentication) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(candidatoService.findByUsuarioId(usuarioId));
    }

    // RF003 — Criar perfil de candidato
    @PostMapping
    @Operation(summary = "Criar perfil de candidato")
    public ResponseEntity<CandidatoResponseDTO> create(
            Authentication authentication,
            @Valid @RequestBody CandidatoRequestDTO dto) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(candidatoService.create(usuarioId, dto));
    }

    // RF003 — Atualizar perfil de candidato
    @PutMapping("/meu-perfil")
    @Operation(summary = "Atualizar meu perfil de candidato")
    public ResponseEntity<CandidatoResponseDTO> update(
            Authentication authentication,
            @Valid @RequestBody CandidatoRequestDTO dto) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(candidatoService.update(usuarioId, dto));
    }

    @PostMapping(value = "/meu-perfil/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Enviar foto de perfil do candidato (JPG, PNG ou WebP — máx. 5 MB)")
    public ResponseEntity<String> uploadFoto(
            Authentication authentication,
            @RequestParam("arquivo") MultipartFile arquivo) {
        Long usuarioId = Long.parseLong(authentication.getName());
        String url = fotoPerfilService.uploadCandidato(usuarioId, arquivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(url);
    }

    @DeleteMapping("/meu-perfil/foto")
    @Operation(summary = "Remover foto de perfil do candidato")
    public ResponseEntity<Void> deletarFoto(Authentication authentication) {
        Long usuarioId = Long.parseLong(authentication.getName());
        fotoPerfilService.deletarCandidato(usuarioId);
        return ResponseEntity.noContent().build();
    }

    // Sugestão de vagas baseada no perfil do candidato
    @GetMapping("/sugestoes")
    @Operation(
        summary = "Vagas sugeridas para o candidato",
        description = "Retorna até 10 vagas ativas ordenadas por compatibilidade com o perfil "
                    + "do candidato (objetivo profissional, pretensão salarial e faixa etária). "
                    + "Vagas já candidatadas são excluídas automaticamente."
    )
    public ResponseEntity<List<SugestaoVagaResponseDTO>> sugestoes(Authentication authentication) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(sugestaoVagaService.sugerirVagas(usuarioId));
    }
}
