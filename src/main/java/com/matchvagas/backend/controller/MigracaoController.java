package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.MigracaoResultadoDTO;
import com.matchvagas.backend.service.MigracaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Migrações operacionais pontuais executadas após o deploy das correções de
 * segurança/LGPD. Fica sob {@code /api/admin/**}, portanto exige autoridade ADMIN
 * (ver SecurityConfig). São idempotentes — seguras para reexecutar.
 */
@RestController
@RequestMapping("/api/admin/migracao")
@RequiredArgsConstructor
@Tag(name = "Migrações (Admin)",
     description = "Migrações operacionais pós-deploy de segurança/LGPD — acesso restrito a ADMIN")
public class MigracaoController {

    private final MigracaoService migracaoService;

    @PostMapping("/backfill-cpf")
    @Operation(
        summary = "Backfill de CPF (LGPD-04)",
        description = "Cifra em repouso os CPFs legados em texto puro e popula cpf_hash "
                    + "para os candidatos sem hash. Idempotente.")
    public ResponseEntity<MigracaoResultadoDTO> backfillCpf() {
        return ResponseEntity.ok(migracaoService.backfillCpf());
    }

    @PostMapping("/normalizar-urls-imagens")
    @Operation(
        summary = "Normalizar URLs de imagem (LGPD-08)",
        description = "Converte URLs públicas antigas de foto de perfil e logo em object path "
                    + "relativo ao bucket (formato do bucket privado + URL assinada). Idempotente.")
    public ResponseEntity<MigracaoResultadoDTO> normalizarUrlsImagens() {
        return ResponseEntity.ok(migracaoService.normalizarUrlsImagens());
    }
}
