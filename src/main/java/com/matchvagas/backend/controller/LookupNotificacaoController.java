package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.TipoNotificacaoRequestDTO;
import com.matchvagas.backend.dto.TipoNotificacaoResponseDTO;
import com.matchvagas.backend.entity.TipoNotificacao;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.TipoNotificacaoMapper;
import com.matchvagas.backend.repository.TipoNotificacaoRepository;
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
@RequestMapping("/api/lookup/notificacoes")
@RequiredArgsConstructor
@Tag(name = "Lookup — Notificações", description = "Tipos de notificação — leitura pública, escrita restrita a ADMIN")
public class LookupNotificacaoController {

    private final TipoNotificacaoRepository tipoNotificacaoRepository;
    private final TipoNotificacaoMapper tipoNotificacaoMapper;

    @GetMapping("/tipos")
    @Operation(summary = "Listar tipos de notificação")
    public ResponseEntity<List<TipoNotificacaoResponseDTO>> listarTipos() {
        return ResponseEntity.ok(tipoNotificacaoRepository.findAll()
                .stream().map(tipoNotificacaoMapper::toResponseDTO).collect(Collectors.toList()));
    }

    @PostMapping("/tipos")
    @Operation(summary = "Cadastrar tipo de notificação — restrito a ADMIN")
    public ResponseEntity<TipoNotificacaoResponseDTO> criarTipo(
            @Valid @RequestBody TipoNotificacaoRequestDTO dto) {
        TipoNotificacao entity = tipoNotificacaoMapper.toEntity(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tipoNotificacaoMapper.toResponseDTO(tipoNotificacaoRepository.save(entity)));
    }

    @PutMapping("/tipos/{id}")
    @Operation(summary = "Atualizar tipo de notificação — restrito a ADMIN")
    public ResponseEntity<TipoNotificacaoResponseDTO> atualizarTipo(
            @PathVariable Integer id, @Valid @RequestBody TipoNotificacaoRequestDTO dto) {
        TipoNotificacao entity = tipoNotificacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de notificação não encontrado"));
        entity.setStatus(dto.status());
        return ResponseEntity.ok(tipoNotificacaoMapper.toResponseDTO(
                tipoNotificacaoRepository.save(entity)));
    }

    @DeleteMapping("/tipos/{id}")
    @Operation(summary = "Remover tipo de notificação — restrito a ADMIN")
    public ResponseEntity<Void> removerTipo(@PathVariable Integer id) {
        if (!tipoNotificacaoRepository.existsById(id))
            throw new ResourceNotFoundException("Tipo de notificação não encontrado");
        tipoNotificacaoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
