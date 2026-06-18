package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.*;
import com.matchvagas.backend.entity.*;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.*;
import com.matchvagas.backend.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/lookup/sistema")
@RequiredArgsConstructor
@Tag(name = "Lookup — Sistema", description = "Status de candidatura, tipos de telefone e departamentos")
public class LookupSistemaController {

    private final StatusCandidaturaRepository statusCandidaturaRepository;
    private final TipoTelefoneRepository tipoTelefoneRepository;
    private final DepartamentosRepository departamentosRepository;

    private final StatusCandidaturaMapper statusCandidaturaMapper;
    private final TipoTelefoneMapper tipoTelefoneMapper;
    private final DepartamentoMapper departamentoMapper;

    // ── Status de Candidatura ─────────────────────────────────────────────────

    @GetMapping("/status-candidatura")
    @Operation(summary = "Listar status de candidatura (ex: EM_ANALISE, APROVADO, REPROVADO)")
    public ResponseEntity<List<StatusCandidaturaResponseDTO>> listarStatus() {
        return ResponseEntity.ok(statusCandidaturaRepository.findAll()
                .stream().map(statusCandidaturaMapper::toResponseDTO).collect(Collectors.toList()));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/status-candidatura")
    @Operation(summary = "Cadastrar status de candidatura — restrito a ADMIN")
    public ResponseEntity<StatusCandidaturaResponseDTO> criarStatus(
            @Valid @RequestBody StatusCandidaturaRequestDTO dto) {
        StatusCandidatura entity = statusCandidaturaMapper.toEntity(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(statusCandidaturaMapper.toResponseDTO(statusCandidaturaRepository.save(entity)));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/status-candidatura/{id}")
    @Operation(summary = "Atualizar status de candidatura — restrito a ADMIN")
    public ResponseEntity<StatusCandidaturaResponseDTO> atualizarStatus(
            @PathVariable Long id, @Valid @RequestBody StatusCandidaturaRequestDTO dto) {
        StatusCandidatura entity = statusCandidaturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Status não encontrado"));
        entity.setStatus(dto.status());
        return ResponseEntity.ok(statusCandidaturaMapper.toResponseDTO(
                statusCandidaturaRepository.save(entity)));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/status-candidatura/{id}")
    @Operation(summary = "Remover status de candidatura — restrito a ADMIN")
    public ResponseEntity<Void> removerStatus(@PathVariable Long id) {
        if (!statusCandidaturaRepository.existsById(id))
            throw new ResourceNotFoundException("Status não encontrado");
        statusCandidaturaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Tipos de Telefone ─────────────────────────────────────────────────────

    @GetMapping("/tipos-telefone")
    @Operation(summary = "Listar tipos de telefone (ex: Celular, Fixo, WhatsApp)")
    public ResponseEntity<List<TipoTelefoneResponseDTO>> listarTiposTelefone() {
        return ResponseEntity.ok(tipoTelefoneRepository.findAll()
                .stream().map(tipoTelefoneMapper::toResponseDTO).collect(Collectors.toList()));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/tipos-telefone")
    @Operation(summary = "Cadastrar tipo de telefone — restrito a ADMIN")
    public ResponseEntity<TipoTelefoneResponseDTO> criarTipoTelefone(
            @Valid @RequestBody TipoTelefoneRequestDTO dto) {
        TipoTelefone entity = tipoTelefoneMapper.toEntity(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tipoTelefoneMapper.toResponseDTO(tipoTelefoneRepository.save(entity)));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/tipos-telefone/{id}")
    @Operation(summary = "Atualizar tipo de telefone — restrito a ADMIN")
    public ResponseEntity<TipoTelefoneResponseDTO> atualizarTipoTelefone(
            @PathVariable Long id, @Valid @RequestBody TipoTelefoneRequestDTO dto) {
        TipoTelefone entity = tipoTelefoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de telefone não encontrado"));
        entity.setNome(dto.nome());
        return ResponseEntity.ok(tipoTelefoneMapper.toResponseDTO(tipoTelefoneRepository.save(entity)));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/tipos-telefone/{id}")
    @Operation(summary = "Remover tipo de telefone — restrito a ADMIN")
    public ResponseEntity<Void> removerTipoTelefone(@PathVariable Long id) {
        if (!tipoTelefoneRepository.existsById(id))
            throw new ResourceNotFoundException("Tipo de telefone não encontrado");
        tipoTelefoneRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Departamentos ─────────────────────────────────────────────────────────

    @GetMapping("/departamentos")
    @Operation(summary = "Listar departamentos (usados em administradores)")
    public ResponseEntity<List<DepartamentoResponseDTO>> listarDepartamentos() {
        return ResponseEntity.ok(departamentosRepository.findAll()
                .stream().map(departamentoMapper::toResponseDTO).collect(Collectors.toList()));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/departamentos")
    @Operation(summary = "Cadastrar departamento — restrito a ADMIN")
    public ResponseEntity<DepartamentoResponseDTO> criarDepartamento(
            @Valid @RequestBody DepartamentoRequestDTO dto) {
        Departamentos entity = departamentoMapper.toEntity(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(departamentoMapper.toResponseDTO(departamentosRepository.save(entity)));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/departamentos/{id}")
    @Operation(summary = "Atualizar departamento — restrito a ADMIN")
    public ResponseEntity<DepartamentoResponseDTO> atualizarDepartamento(
            @PathVariable Long id, @Valid @RequestBody DepartamentoRequestDTO dto) {
        Departamentos entity = departamentosRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Departamento não encontrado"));
        entity.setNome(dto.nome());
        entity.setDescricao(dto.descricao());
        return ResponseEntity.ok(departamentoMapper.toResponseDTO(departamentosRepository.save(entity)));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/departamentos/{id}")
    @Operation(summary = "Remover departamento — restrito a ADMIN")
    public ResponseEntity<Void> removerDepartamento(@PathVariable Long id) {
        if (!departamentosRepository.existsById(id))
            throw new ResourceNotFoundException("Departamento não encontrado");
        departamentosRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
