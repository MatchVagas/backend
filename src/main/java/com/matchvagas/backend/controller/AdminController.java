package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.AdministradorRequestDTO;
import com.matchvagas.backend.dto.AdministradorResponseDTO;
import com.matchvagas.backend.dto.CandidatoResponseDTO;
import com.matchvagas.backend.dto.EmpresaResponseDTO;
import com.matchvagas.backend.dto.UsuarioResponseDTO;
import com.matchvagas.backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Administração", description = "Gestão global do sistema (RF010) — acesso restrito a ADMIN")
public class AdminController {

    private final AdminService adminService;

    // ── Gestão de Usuários ──────────────────────────────────────────────────

    @GetMapping("/usuarios")
    @Operation(summary = "Listar todos os usuários do sistema")
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
        return ResponseEntity.ok(adminService.listarUsuarios());
    }

    @PatchMapping("/usuarios/{id}/ativar")
    @Operation(summary = "Ativar usuário")
    public ResponseEntity<Void> ativarUsuario(@PathVariable Long id) {
        adminService.ativarDesativarUsuario(id, true);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/usuarios/{id}/desativar")
    @Operation(summary = "Desativar usuário")
    public ResponseEntity<Void> desativarUsuario(@PathVariable Long id) {
        adminService.ativarDesativarUsuario(id, false);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/usuarios/{id}")
    @Operation(summary = "Excluir usuário do sistema")
    public ResponseEntity<Void> excluirUsuario(@PathVariable Long id) {
        adminService.excluirUsuario(id);
        return ResponseEntity.noContent().build();
    }

    // ── Gestão de Candidatos ────────────────────────────────────────────────

    @GetMapping("/candidatos")
    @Operation(summary = "Listar todos os candidatos")
    public ResponseEntity<List<CandidatoResponseDTO>> listarCandidatos() {
        return ResponseEntity.ok(adminService.listarCandidatos());
    }

    // ── Gestão de Empresas ──────────────────────────────────────────────────

    @GetMapping("/empresas")
    @Operation(summary = "Listar todas as empresas")
    public ResponseEntity<List<EmpresaResponseDTO>> listarEmpresas() {
        return ResponseEntity.ok(adminService.listarEmpresas());
    }

    // ── Gestão de Administradores ───────────────────────────────────────────

    @GetMapping("/admins")
    @Operation(summary = "Listar todos os administradores")
    public ResponseEntity<List<AdministradorResponseDTO>> listarAdmins() {
        return ResponseEntity.ok(adminService.listarAdmins());
    }

    @PostMapping("/admins")
    @Operation(summary = "Promover usuário a administrador")
    public ResponseEntity<AdministradorResponseDTO> criarAdmin(
            @Valid @RequestBody AdministradorRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.criarAdmin(dto));
    }

    @DeleteMapping("/admins/{id}")
    @Operation(summary = "Remover administrador")
    public ResponseEntity<Void> removerAdmin(@PathVariable Long id) {
        adminService.removerAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
