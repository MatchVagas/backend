package com.matchvagas.backend.controller;

import com.matchvagas.backend.CandidaturaRequest;
import com.matchvagas.backend.CandidaturaResponse;
import com.matchvagas.backend.service.CandidaturaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/candidaturas")
public class CandidaduraController {

    private final CandidaturaService candidaturaService;

    public CandidaduraController(CandidaturaService candidaturaService) {
        this.candidaturaService = candidaturaService;
    }

    @PostMapping
    public ResponseEntity<CandidaturaResponse> criarCandidatura(
            @Validated @RequestBody CandidaturaRequest request,
            @AuthenticationPrincipal UserDetails usuario) {

        CandidaturaResponse created = candidaturaService.criar(request, usuario.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/minhas")
    public ResponseEntity<List<CandidaturaResponse>> listarMinhas(
            @AuthenticationPrincipal UserDetails usuario) {

        List<CandidaturaResponse> lista = candidaturaService.listarPorUsuario(usuario.getUsername());
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CandidaturaResponse> buscarPorId(@PathVariable Long id,
                                                           @AuthenticationPrincipal UserDetails usuario) {

        CandidaturaResponse resp = candidaturaService.buscarPorId(id, usuario.getUsername());
        return ResponseEntity.ok(resp);
    }
}
