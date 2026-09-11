package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.CurriculoResponseDTO;
import com.matchvagas.backend.dto.ResumoPerfilResponseDTO;
import com.matchvagas.backend.service.CurriculoService;
import com.matchvagas.backend.service.assistente.ResumoPerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/candidatos/curriculo")
@RequiredArgsConstructor
@Tag(name = "Currículo", description = "Upload e gerenciamento do currículo do candidato")
public class CurriculoController {

    private final CurriculoService curriculoService;
    private final ResumoPerfilService resumoPerfilService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Enviar ou substituir currículo (PDF, DOC ou DOCX — máx. 5 MB)")
    public ResponseEntity<CurriculoResponseDTO> upload(
            Authentication authentication,
            @RequestParam("arquivo") MultipartFile arquivo) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(curriculoService.upload(usuarioId, arquivo));
    }

    @GetMapping("/download")
    @Operation(summary = "Redireciona para URL assinada do currículo (válida por 1 hora)")
    public ResponseEntity<Void> download(Authentication authentication) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return curriculoService.download(usuarioId);
    }

    @GetMapping
    @Operation(summary = "Consultar metadados do currículo cadastrado")
    public ResponseEntity<CurriculoResponseDTO> buscar(Authentication authentication) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(curriculoService.buscar(usuarioId));
    }

    @DeleteMapping
    @Operation(summary = "Remover currículo cadastrado")
    public ResponseEntity<Void> deletar(Authentication authentication) {
        Long usuarioId = Long.parseLong(authentication.getName());
        curriculoService.deletar(usuarioId);
        return ResponseEntity.noContent().build();
    }

    // ── Fase 3 — assistente de IA (LLM local) ────────────────────────────────

    @PostMapping("/resumo")
    @Operation(
        summary = "Sugerir objetivo profissional e habilidades a partir do currículo",
        description = "Gerado por um LLM local; não altera o perfil — o candidato revisa e aplica. "
                    + "Reaproveita a sugestão já gerada para o mesmo currículo, salvo com regenerar=true. "
                    + "Responde 503 quando o assistente está desligado ou indisponível."
    )
    public ResponseEntity<ResumoPerfilResponseDTO> gerarResumo(
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean regenerar) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(resumoPerfilService.gerar(usuarioId, regenerar));
    }

    @GetMapping("/resumo")
    @Operation(summary = "Consultar a última sugestão gerada para o currículo atual")
    public ResponseEntity<ResumoPerfilResponseDTO> buscarResumo(Authentication authentication) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(resumoPerfilService.buscar(usuarioId));
    }
}
