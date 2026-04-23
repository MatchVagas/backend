package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.*;
import com.matchvagas.backend.entity.Cidade;
import com.matchvagas.backend.entity.Estado;
import com.matchvagas.backend.entity.Pais;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.CidadeMapper;
import com.matchvagas.backend.mapper.EstadoMapper;
import com.matchvagas.backend.mapper.PaisMapper;
import com.matchvagas.backend.repository.CidadeRepository;
import com.matchvagas.backend.repository.EstadoRepository;
import com.matchvagas.backend.repository.PaisRepository;
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
@RequestMapping("/api/localizacao")
@RequiredArgsConstructor
@Tag(name = "Localização", description = "Países, Estados e Cidades — dados auxiliares públicos")
public class LocalizacaoController {

    private final PaisRepository paisRepository;
    private final EstadoRepository estadoRepository;
    private final CidadeRepository cidadeRepository;
    private final PaisMapper paisMapper;
    private final EstadoMapper estadoMapper;
    private final CidadeMapper cidadeMapper;

    // ── Países ────────────────────────────────────────────────────────────────

    @GetMapping("/paises")
    @Operation(summary = "Listar todos os países")
    public ResponseEntity<List<PaisResponseDTO>> listarPaises() {
        List<PaisResponseDTO> result = paisRepository.findAll()
                .stream().map(paisMapper::toResponseDTO).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/paises/{id}")
    @Operation(summary = "Buscar país por ID")
    public ResponseEntity<PaisResponseDTO> buscarPais(@PathVariable Long id) {
        Pais pais = paisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("País não encontrado com ID: " + id));
        return ResponseEntity.ok(paisMapper.toResponseDTO(pais));
    }

    @PostMapping("/paises")
    @Operation(summary = "Cadastrar país — restrito a ADMIN")
    public ResponseEntity<PaisResponseDTO> criarPais(@Valid @RequestBody PaisRequestDTO dto) {
        Pais pais = paisMapper.toEntity(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paisMapper.toResponseDTO(paisRepository.save(pais)));
    }

    @PutMapping("/paises/{id}")
    @Operation(summary = "Atualizar país — restrito a ADMIN")
    public ResponseEntity<PaisResponseDTO> atualizarPais(
            @PathVariable Long id, @Valid @RequestBody PaisRequestDTO dto) {
        Pais pais = paisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("País não encontrado com ID: " + id));
        pais.setNome(dto.nome());
        pais.setCodigoIso(dto.codigoIso());
        return ResponseEntity.ok(paisMapper.toResponseDTO(paisRepository.save(pais)));
    }

    @DeleteMapping("/paises/{id}")
    @Operation(summary = "Remover país — restrito a ADMIN")
    public ResponseEntity<Void> removerPais(@PathVariable Long id) {
        if (!paisRepository.existsById(id))
            throw new ResourceNotFoundException("País não encontrado com ID: " + id);
        paisRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Estados ───────────────────────────────────────────────────────────────

    @GetMapping("/estados")
    @Operation(summary = "Listar todos os estados")
    public ResponseEntity<List<EstadoResponseDTO>> listarEstados(
            @RequestParam(required = false) Long paisId) {
        List<Estado> estados = paisId != null
                ? estadoRepository.findByPaisId(paisId)
                : estadoRepository.findAll();
        List<EstadoResponseDTO> result = estados.stream()
                .map(estadoMapper::toResponseDTO).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/estados/{id}")
    @Operation(summary = "Buscar estado por ID")
    public ResponseEntity<EstadoResponseDTO> buscarEstado(@PathVariable Long id) {
        Estado estado = estadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estado não encontrado com ID: " + id));
        return ResponseEntity.ok(estadoMapper.toResponseDTO(estado));
    }

    @PostMapping("/estados")
    @Operation(summary = "Cadastrar estado — restrito a ADMIN")
    public ResponseEntity<EstadoResponseDTO> criarEstado(@Valid @RequestBody EstadoRequestDTO dto) {
        Estado estado = estadoMapper.toEntity(dto);
        if (dto.paisId() != null) {
            Pais pais = paisRepository.findById(dto.paisId())
                    .orElseThrow(() -> new ResourceNotFoundException("País não encontrado"));
            estado.setPais(pais);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(estadoMapper.toResponseDTO(estadoRepository.save(estado)));
    }

    @PutMapping("/estados/{id}")
    @Operation(summary = "Atualizar estado — restrito a ADMIN")
    public ResponseEntity<EstadoResponseDTO> atualizarEstado(
            @PathVariable Long id, @Valid @RequestBody EstadoRequestDTO dto) {
        Estado estado = estadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estado não encontrado com ID: " + id));
        estado.setNome(dto.nome());
        estado.setUf(dto.uf());
        if (dto.paisId() != null) {
            Pais pais = paisRepository.findById(dto.paisId())
                    .orElseThrow(() -> new ResourceNotFoundException("País não encontrado"));
            estado.setPais(pais);
        }
        return ResponseEntity.ok(estadoMapper.toResponseDTO(estadoRepository.save(estado)));
    }

    @DeleteMapping("/estados/{id}")
    @Operation(summary = "Remover estado — restrito a ADMIN")
    public ResponseEntity<Void> removerEstado(@PathVariable Long id) {
        if (!estadoRepository.existsById(id))
            throw new ResourceNotFoundException("Estado não encontrado com ID: " + id);
        estadoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Cidades ───────────────────────────────────────────────────────────────

    @GetMapping("/cidades")
    @Operation(summary = "Listar cidades, opcionalmente filtradas por estado")
    public ResponseEntity<List<CidadeResponseDTO>> listarCidades(
            @RequestParam(required = false) Long estadoId) {
        List<Cidade> cidades = estadoId != null
                ? cidadeRepository.findByEstado(estadoRepository.findById(estadoId)
                        .orElseThrow(() -> new ResourceNotFoundException("Estado não encontrado")))
                : cidadeRepository.findAll();
        List<CidadeResponseDTO> result = cidades.stream()
                .map(cidadeMapper::toResponseDTO).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/cidades/{id}")
    @Operation(summary = "Buscar cidade por ID")
    public ResponseEntity<CidadeResponseDTO> buscarCidade(@PathVariable Long id) {
        Cidade cidade = cidadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cidade não encontrada com ID: " + id));
        return ResponseEntity.ok(cidadeMapper.toResponseDTO(cidade));
    }

    @PostMapping("/cidades")
    @Operation(summary = "Cadastrar cidade — restrito a ADMIN")
    public ResponseEntity<CidadeResponseDTO> criarCidade(@Valid @RequestBody CidadeRequestDTO dto) {
        Cidade cidade = cidadeMapper.toEntity(dto);
        if (dto.estadoId() != null) {
            Estado estado = estadoRepository.findById(dto.estadoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Estado não encontrado"));
            cidade.setEstado(estado);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cidadeMapper.toResponseDTO(cidadeRepository.save(cidade)));
    }

    @PutMapping("/cidades/{id}")
    @Operation(summary = "Atualizar cidade — restrito a ADMIN")
    public ResponseEntity<CidadeResponseDTO> atualizarCidade(
            @PathVariable Long id, @Valid @RequestBody CidadeRequestDTO dto) {
        Cidade cidade = cidadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cidade não encontrada com ID: " + id));
        cidade.setNome(dto.nome());
        if (dto.estadoId() != null) {
            Estado estado = estadoRepository.findById(dto.estadoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Estado não encontrado"));
            cidade.setEstado(estado);
        }
        return ResponseEntity.ok(cidadeMapper.toResponseDTO(cidadeRepository.save(cidade)));
    }

    @DeleteMapping("/cidades/{id}")
    @Operation(summary = "Remover cidade — restrito a ADMIN")
    public ResponseEntity<Void> removerCidade(@PathVariable Long id) {
        if (!cidadeRepository.existsById(id))
            throw new ResourceNotFoundException("Cidade não encontrada com ID: " + id);
        cidadeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
