package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.AtualizarCompartilhamentoRequestDTO;
import com.matchvagas.backend.dto.CandidaturaEmpresaResponseDTO;
import com.matchvagas.backend.dto.CandidaturaRequestDTO;
import com.matchvagas.backend.dto.CandidaturaResponseDTO;
import com.matchvagas.backend.dto.HistoricoStatusResponseDTO;
import com.matchvagas.backend.service.CandidaturaService;
import com.matchvagas.backend.service.CurriculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/candidaturas")
@RequiredArgsConstructor
@Tag(name = "Candidaturas", description = "Candidatura a vagas com controle de privacidade (RF008, RF009)")
public class CandidaturaController {

    private final CandidaturaService candidaturaService;
    private final CurriculoService   curriculoService;

    // ── RF008 — Candidatar-se a uma vaga ─────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAuthority('CANDIDATO')")
    @Operation(
        summary = "Candidatar-se a uma vaga",
        description = "O candidato pode definir quais dados serão visíveis para a empresa. "
                    + "Campos não informados usam o padrão: dados profissionais compartilhados, "
                    + "telefone e endereço privados."
    )
    public ResponseEntity<CandidaturaResponseDTO> candidatar(
            Authentication authentication,
            @Valid @RequestBody CandidaturaRequestDTO request) {
        Long candidatoId = Long.parseLong(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(candidaturaService.candidatar(candidatoId, request));
    }

    // ── RF009 — Listar minhas candidaturas ───────────────────────────────────

    @GetMapping("/minhas")
    @PreAuthorize("hasAuthority('CANDIDATO')")
    @Operation(summary = "Listar minhas candidaturas com preferências de compartilhamento")
    public ResponseEntity<List<CandidaturaResponseDTO>> minhasCandidaturas(
            Authentication authentication) {
        Long candidatoId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(candidaturaService.findByCandidato(candidatoId));
    }

    // ── RF009 — Detalhar uma candidatura ─────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CANDIDATO')")
    @Operation(summary = "Detalhar candidatura com preferências de compartilhamento atuais")
    public ResponseEntity<CandidaturaResponseDTO> findById(
            @PathVariable Long id,
            Authentication authentication) {
        Long candidatoId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(candidaturaService.findByIdAndCandidato(id, candidatoId));
    }

    // ── Atualizar preferências de compartilhamento ────────────────────────────

    @PatchMapping("/{id}/compartilhamento")
    @PreAuthorize("hasAuthority('CANDIDATO')")
    @Operation(
        summary = "Atualizar o que o candidato compartilha com a empresa",
        description = "Permite que o candidato altere, a qualquer momento, quais dados "
                    + "da sua candidatura ficam visíveis para a empresa recrutadora."
    )
    public ResponseEntity<CandidaturaResponseDTO> atualizarCompartilhamento(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody AtualizarCompartilhamentoRequestDTO dto) {
        Long candidatoId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(
                candidaturaService.atualizarCompartilhamento(id, candidatoId, dto));
    }

    // ── Empresa — listar candidaturas recebidas ───────────────────────────────

    @GetMapping("/empresa")
    @PreAuthorize("hasAuthority('EMPRESA')")
    @Operation(
        summary = "Listar candidaturas recebidas nas vagas da empresa",
        description = "Retorna somente os dados que cada candidato autorizou compartilhar. "
                    + "Campos não compartilhados são omitidos da resposta."
    )
    public ResponseEntity<List<CandidaturaEmpresaResponseDTO>> candidaturasEmpresa(
            Authentication authentication) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(candidaturaService.findByEmpresa(usuarioId));
    }

    // ── Empresa — candidatos de uma vaga específica ───────────────────────────

    @GetMapping("/empresa/vaga/{vagaId}")
    @PreAuthorize("hasAuthority('EMPRESA')")
    @Operation(
        summary = "Listar candidatos de uma vaga específica",
        description = "Retorna todos os candidatos que se inscreveram em uma vaga da empresa, "
                    + "exibindo somente os dados que cada candidato autorizou compartilhar."
    )
    public ResponseEntity<List<CandidaturaEmpresaResponseDTO>> candidatosPorVaga(
            @PathVariable Long vagaId,
            Authentication authentication) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(candidaturaService.findByVagaAndEmpresa(vagaId, usuarioId));
    }

    // ── Empresa — detalhar candidatura específica ─────────────────────────────

    @GetMapping("/{id}/empresa")
    @PreAuthorize("hasAuthority('EMPRESA')")
    @Operation(
        summary = "Detalhar uma candidatura específica (visão da empresa)",
        description = "Retorna o detalhe de uma candidatura mostrando somente o que "
                    + "o candidato autorizou compartilhar."
    )
    public ResponseEntity<CandidaturaEmpresaResponseDTO> detalharCandidaturaEmpresa(
            @PathVariable Long id,
            Authentication authentication) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(candidaturaService.findByIdAndEmpresa(id, usuarioId));
    }

    // ── Empresa — download do currículo do candidato ──────────────────────────

    @GetMapping("/{id}/curriculo/download")
    @PreAuthorize("hasAuthority('EMPRESA')")
    @Operation(
        summary = "Baixar currículo do candidato (visão da empresa)",
        description = """
            Permite que a empresa baixe o currículo do candidato. \
            Bloqueado automaticamente se:
            - A candidatura não pertencer a uma vaga da empresa;
            - O candidato não tiver autorizado o compartilhamento do currículo;
            - O candidato não possuir currículo cadastrado.
            """
    )
    public ResponseEntity<Void> downloadCurriculoCandidato(
            @PathVariable Long id,
            Authentication authentication) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return curriculoService.downloadParaEmpresa(id, usuarioId);
    }

    // ── Empresa — atualizar status da candidatura ─────────────────────────────

    @PatchMapping("/{id}/empresa/status/{statusId}")
    @PreAuthorize("hasAuthority('EMPRESA')")
    @Operation(summary = "Atualizar status de uma candidatura (visão da empresa)")
    public ResponseEntity<CandidaturaEmpresaResponseDTO> atualizarStatusEmpresa(
            @PathVariable Long id,
            @PathVariable Long statusId,
            Authentication authentication) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(candidaturaService.atualizarStatusEmpresa(id, statusId, usuarioId));
    }

    // ── Empresa — histórico de mudanças de status ─────────────────────────────

    @GetMapping("/{id}/empresa/historico")
    @PreAuthorize("hasAuthority('EMPRESA')")
    @Operation(
        summary = "Listar o histórico de mudanças de status de uma candidatura",
        description = "Trilha de auditoria com status anterior, novo status, quem efetuou "
                    + "a mudança e quando, em ordem decrescente de data."
    )
    public ResponseEntity<List<HistoricoStatusResponseDTO>> historicoStatusEmpresa(
            @PathVariable Long id,
            Authentication authentication) {
        Long usuarioId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(candidaturaService.listarHistoricoEmpresa(id, usuarioId));
    }
}
