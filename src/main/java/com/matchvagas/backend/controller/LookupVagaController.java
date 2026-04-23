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

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/lookup/vagas")
@RequiredArgsConstructor
@Tag(name = "Lookup — Vagas", description = "Dados auxiliares para cadastro de vagas: tipo, modalidade, escolaridade, porte, ramo e status")
public class LookupVagaController {

    private final TipoVagaRepository tipoVagaRepository;
    private final ModalidadeRepository modalidadeRepository;
    private final EscolaridadeRepository escolaridadeRepository;
    private final PorteRepository porteRepository;
    private final RamoAtuacaoRepository ramoAtuacaoRepository;
    private final StatusVagaRepository statusVagaRepository;

    private final TipoVagaMapper tipoVagaMapper;
    private final ModalidadeMapper modalidadeMapper;
    private final EscolaridadeMapper escolaridadeMapper;
    private final PorteMapper porteMapper;
    private final RamoAtuacaoMapper ramoAtuacaoMapper;
    private final StatusVagaMapper statusVagaMapper;

    // ── Tipos de Vaga ─────────────────────────────────────────────────────────

    @GetMapping("/tipos")
    @Operation(summary = "Listar tipos de vaga (ex: CLT, PJ, Estágio)")
    public ResponseEntity<List<TipoVagaResponseDTO>> listarTipos() {
        return ResponseEntity.ok(tipoVagaRepository.findAll()
                .stream().map(tipoVagaMapper::toResponseDTO).collect(Collectors.toList()));
    }

    @PostMapping("/tipos")
    @Operation(summary = "Cadastrar tipo de vaga — restrito a ADMIN")
    public ResponseEntity<TipoVagaResponseDTO> criarTipo(@Valid @RequestBody TipoVagaRequestDTO dto) {
        TipoVaga entity = tipoVagaMapper.toEntity(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tipoVagaMapper.toResponseDTO(tipoVagaRepository.save(entity)));
    }

    @PutMapping("/tipos/{id}")
    @Operation(summary = "Atualizar tipo de vaga — restrito a ADMIN")
    public ResponseEntity<TipoVagaResponseDTO> atualizarTipo(
            @PathVariable Long id, @Valid @RequestBody TipoVagaRequestDTO dto) {
        TipoVaga entity = tipoVagaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de vaga não encontrado"));
        entity.setDescricao(dto.descricao());
        return ResponseEntity.ok(tipoVagaMapper.toResponseDTO(tipoVagaRepository.save(entity)));
    }

    @DeleteMapping("/tipos/{id}")
    @Operation(summary = "Remover tipo de vaga — restrito a ADMIN")
    public ResponseEntity<Void> removerTipo(@PathVariable Long id) {
        if (!tipoVagaRepository.existsById(id))
            throw new ResourceNotFoundException("Tipo de vaga não encontrado");
        tipoVagaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Modalidades ───────────────────────────────────────────────────────────

    @GetMapping("/modalidades")
    @Operation(summary = "Listar modalidades (ex: Presencial, Remoto, Híbrido)")
    public ResponseEntity<List<ModalidadeResponseDTO>> listarModalidades() {
        return ResponseEntity.ok(modalidadeRepository.findAll()
                .stream().map(modalidadeMapper::toResponseDTO).collect(Collectors.toList()));
    }

    @PostMapping("/modalidades")
    @Operation(summary = "Cadastrar modalidade — restrito a ADMIN")
    public ResponseEntity<ModalidadeResponseDTO> criarModalidade(@Valid @RequestBody ModalidadeRequestDTO dto) {
        Modalidade entity = modalidadeMapper.toEntity(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(modalidadeMapper.toResponseDTO(modalidadeRepository.save(entity)));
    }

    @PutMapping("/modalidades/{id}")
    @Operation(summary = "Atualizar modalidade — restrito a ADMIN")
    public ResponseEntity<ModalidadeResponseDTO> atualizarModalidade(
            @PathVariable Long id, @Valid @RequestBody ModalidadeRequestDTO dto) {
        Modalidade entity = modalidadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modalidade não encontrada"));
        entity.setDescricao(dto.descricao());
        return ResponseEntity.ok(modalidadeMapper.toResponseDTO(modalidadeRepository.save(entity)));
    }

    @DeleteMapping("/modalidades/{id}")
    @Operation(summary = "Remover modalidade — restrito a ADMIN")
    public ResponseEntity<Void> removerModalidade(@PathVariable Long id) {
        if (!modalidadeRepository.existsById(id))
            throw new ResourceNotFoundException("Modalidade não encontrada");
        modalidadeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Escolaridades ─────────────────────────────────────────────────────────

    @GetMapping("/escolaridades")
    @Operation(summary = "Listar níveis de escolaridade ordenados")
    public ResponseEntity<List<EscolaridadeResponseDTO>> listarEscolaridades() {
        return ResponseEntity.ok(escolaridadeRepository.findAll()
                .stream().map(escolaridadeMapper::toResponseDTO).collect(Collectors.toList()));
    }

    @PostMapping("/escolaridades")
    @Operation(summary = "Cadastrar nível de escolaridade — restrito a ADMIN")
    public ResponseEntity<EscolaridadeResponseDTO> criarEscolaridade(
            @Valid @RequestBody EscolaridadeRequestDTO dto) {
        Escolaridades entity = escolaridadeMapper.toEntity(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(escolaridadeMapper.toResponseDTO(escolaridadeRepository.save(entity)));
    }

    @PutMapping("/escolaridades/{id}")
    @Operation(summary = "Atualizar escolaridade — restrito a ADMIN")
    public ResponseEntity<EscolaridadeResponseDTO> atualizarEscolaridade(
            @PathVariable Long id, @Valid @RequestBody EscolaridadeRequestDTO dto) {
        Escolaridades entity = escolaridadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Escolaridade não encontrada"));
        entity.setNome(dto.nome());
        entity.setOrdem(dto.ordem());
        return ResponseEntity.ok(escolaridadeMapper.toResponseDTO(escolaridadeRepository.save(entity)));
    }

    @DeleteMapping("/escolaridades/{id}")
    @Operation(summary = "Remover escolaridade — restrito a ADMIN")
    public ResponseEntity<Void> removerEscolaridade(@PathVariable Long id) {
        if (!escolaridadeRepository.existsById(id))
            throw new ResourceNotFoundException("Escolaridade não encontrada");
        escolaridadeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Portes de Empresa ─────────────────────────────────────────────────────

    @GetMapping("/portes")
    @Operation(summary = "Listar portes de empresa (ex: MEI, Pequeno, Médio, Grande)")
    public ResponseEntity<List<PorteResponseDTO>> listarPortes() {
        return ResponseEntity.ok(porteRepository.findAll()
                .stream().map(porteMapper::toResponseDTO).collect(Collectors.toList()));
    }

    @PostMapping("/portes")
    @Operation(summary = "Cadastrar porte — restrito a ADMIN")
    public ResponseEntity<PorteResponseDTO> criarPorte(@Valid @RequestBody PorteRequestDTO dto) {
        Porte entity = porteMapper.toEntity(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(porteMapper.toResponseDTO(porteRepository.save(entity)));
    }

    @PutMapping("/portes/{id}")
    @Operation(summary = "Atualizar porte — restrito a ADMIN")
    public ResponseEntity<PorteResponseDTO> atualizarPorte(
            @PathVariable Long id, @Valid @RequestBody PorteRequestDTO dto) {
        Porte entity = porteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Porte não encontrado"));
        entity.setDescricao(dto.descricao());
        return ResponseEntity.ok(porteMapper.toResponseDTO(porteRepository.save(entity)));
    }

    @DeleteMapping("/portes/{id}")
    @Operation(summary = "Remover porte — restrito a ADMIN")
    public ResponseEntity<Void> removerPorte(@PathVariable Long id) {
        if (!porteRepository.existsById(id))
            throw new ResourceNotFoundException("Porte não encontrado");
        porteRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Ramos de Atuação ──────────────────────────────────────────────────────

    @GetMapping("/ramos")
    @Operation(summary = "Listar ramos de atuação (ex: Tecnologia, Saúde, Educação)")
    public ResponseEntity<List<RamoAtuacaoResponseDTO>> listarRamos() {
        return ResponseEntity.ok(ramoAtuacaoRepository.findAll()
                .stream().map(ramoAtuacaoMapper::toResponseDTO).collect(Collectors.toList()));
    }

    @PostMapping("/ramos")
    @Operation(summary = "Cadastrar ramo de atuação — restrito a ADMIN")
    public ResponseEntity<RamoAtuacaoResponseDTO> criarRamo(@Valid @RequestBody RamoAtuacaoRequestDTO dto) {
        RamoAtuacao entity = ramoAtuacaoMapper.toEntity(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ramoAtuacaoMapper.toResponseDTO(ramoAtuacaoRepository.save(entity)));
    }

    @PutMapping("/ramos/{id}")
    @Operation(summary = "Atualizar ramo de atuação — restrito a ADMIN")
    public ResponseEntity<RamoAtuacaoResponseDTO> atualizarRamo(
            @PathVariable Long id, @Valid @RequestBody RamoAtuacaoRequestDTO dto) {
        RamoAtuacao entity = ramoAtuacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ramo não encontrado"));
        entity.setDescricao(dto.descricao());
        return ResponseEntity.ok(ramoAtuacaoMapper.toResponseDTO(ramoAtuacaoRepository.save(entity)));
    }

    @DeleteMapping("/ramos/{id}")
    @Operation(summary = "Remover ramo de atuação — restrito a ADMIN")
    public ResponseEntity<Void> removerRamo(@PathVariable Long id) {
        if (!ramoAtuacaoRepository.existsById(id))
            throw new ResourceNotFoundException("Ramo não encontrado");
        ramoAtuacaoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Status de Vaga ────────────────────────────────────────────────────────

    @GetMapping("/status")
    @Operation(summary = "Listar status de vaga (ex: ATIVA, ENCERRADA, PAUSADA)")
    public ResponseEntity<List<StatusVagaResponseDTO>> listarStatusVaga() {
        return ResponseEntity.ok(statusVagaRepository.findAll()
                .stream().map(statusVagaMapper::toResponseDTO).collect(Collectors.toList()));
    }

    @PostMapping("/status")
    @Operation(summary = "Cadastrar status de vaga — restrito a ADMIN")
    public ResponseEntity<StatusVagaResponseDTO> criarStatusVaga(
            @Valid @RequestBody StatusVagaRequestDTO dto) {
        StatusVaga entity = statusVagaMapper.toEntity(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(statusVagaMapper.toResponseDTO(statusVagaRepository.save(entity)));
    }

    @PutMapping("/status/{id}")
    @Operation(summary = "Atualizar status de vaga — restrito a ADMIN")
    public ResponseEntity<StatusVagaResponseDTO> atualizarStatusVaga(
            @PathVariable Long id, @Valid @RequestBody StatusVagaRequestDTO dto) {
        StatusVaga entity = statusVagaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Status não encontrado"));
        entity.setDescricao(dto.descricao());
        return ResponseEntity.ok(statusVagaMapper.toResponseDTO(statusVagaRepository.save(entity)));
    }

    @DeleteMapping("/status/{id}")
    @Operation(summary = "Remover status de vaga — restrito a ADMIN")
    public ResponseEntity<Void> removerStatusVaga(@PathVariable Long id) {
        if (!statusVagaRepository.existsById(id))
            throw new ResourceNotFoundException("Status não encontrado");
        statusVagaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
