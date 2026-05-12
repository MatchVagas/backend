package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.*;
import com.matchvagas.backend.entity.Usuarios;
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
@Tag(name = "Administração", description = "Gestão global do sistema — acesso restrito a ADMIN")
public class AdminController {

    private final AdminService adminService;

    // ═══════════════════════════════════════════════════════════
    //  USUÁRIOS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/usuarios")
    @Operation(summary = "Listar todos os usuários do sistema")
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
        return ResponseEntity.ok(adminService.listarUsuarios());
    }

    @GetMapping("/usuarios/{id}")
    @Operation(summary = "Buscar usuário por ID")
    public ResponseEntity<UsuarioResponseDTO> buscarUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.buscarUsuario(id));
    }

    @PutMapping("/usuarios/{id}")
    @Operation(summary = "Atualizar dados de um usuário")
    public ResponseEntity<UsuarioResponseDTO> atualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody AdminAtualizarUsuarioRequestDTO dto) {
        return ResponseEntity.ok(adminService.atualizarUsuario(id, dto));
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

    @PatchMapping("/usuarios/{id}/tipo/{tipo}")
    @Operation(summary = "Alterar tipo do usuário (CANDIDATO, EMPRESA, ADMIN)")
    public ResponseEntity<Void> alterarTipoUsuario(
            @PathVariable Long id,
            @PathVariable Usuarios.TipoUsuario tipo) {
        adminService.alterarTipoUsuario(id, tipo);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/usuarios/{id}")
    @Operation(summary = "Excluir usuário do sistema")
    public ResponseEntity<Void> excluirUsuario(@PathVariable Long id) {
        adminService.excluirUsuario(id);
        return ResponseEntity.noContent().build();
    }

    // ═══════════════════════════════════════════════════════════
    //  CANDIDATOS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/candidatos")
    @Operation(summary = "Listar todos os candidatos")
    public ResponseEntity<List<CandidatoResponseDTO>> listarCandidatos() {
        return ResponseEntity.ok(adminService.listarCandidatos());
    }

    @GetMapping("/candidatos/{id}")
    @Operation(summary = "Buscar candidato por ID")
    public ResponseEntity<CandidatoResponseDTO> buscarCandidato(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.buscarCandidato(id));
    }

    @PutMapping("/candidatos/{id}")
    @Operation(summary = "Atualizar perfil de candidato")
    public ResponseEntity<CandidatoResponseDTO> atualizarCandidato(
            @PathVariable Long id,
            @Valid @RequestBody CandidatoRequestDTO dto) {
        return ResponseEntity.ok(adminService.atualizarCandidato(id, dto));
    }

    @DeleteMapping("/candidatos/{id}")
    @Operation(summary = "Excluir perfil de candidato")
    public ResponseEntity<Void> excluirCandidato(@PathVariable Long id) {
        adminService.excluirCandidato(id);
        return ResponseEntity.noContent().build();
    }

    // ═══════════════════════════════════════════════════════════
    //  EMPRESAS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/empresas")
    @Operation(summary = "Listar todas as empresas (todos os status)")
    public ResponseEntity<List<EmpresaResponseDTO>> listarEmpresas() {
        return ResponseEntity.ok(adminService.listarEmpresas());
    }

    @GetMapping("/empresas/pendentes")
    @Operation(summary = "Listar empresas aguardando aprovação")
    public ResponseEntity<List<EmpresaResponseDTO>> listarEmpresasPendentes() {
        return ResponseEntity.ok(adminService.listarEmpresasPendentes());
    }

    @GetMapping("/empresas/{id}")
    @Operation(summary = "Buscar empresa por ID")
    public ResponseEntity<EmpresaResponseDTO> buscarEmpresa(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.buscarEmpresa(id));
    }

    @PatchMapping("/empresas/{id}/aprovar")
    @Operation(summary = "Aprovar cadastro de empresa")
    public ResponseEntity<EmpresaResponseDTO> aprovarEmpresa(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.aprovarEmpresa(id));
    }

    @PatchMapping("/empresas/{id}/rejeitar")
    @Operation(summary = "Rejeitar cadastro de empresa")
    public ResponseEntity<EmpresaResponseDTO> rejeitarEmpresa(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.rejeitarEmpresa(id));
    }

    @DeleteMapping("/empresas/{id}")
    @Operation(summary = "Excluir empresa do sistema")
    public ResponseEntity<Void> excluirEmpresa(@PathVariable Long id) {
        adminService.excluirEmpresa(id);
        return ResponseEntity.noContent().build();
    }

    // ═══════════════════════════════════════════════════════════
    //  VAGAS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/vagas")
    @Operation(summary = "Listar todas as vagas do sistema")
    public ResponseEntity<List<VagaResponseDTO>> listarVagas() {
        return ResponseEntity.ok(adminService.listarVagas());
    }

    @GetMapping("/vagas/{id}")
    @Operation(summary = "Buscar vaga por ID")
    public ResponseEntity<VagaResponseDTO> buscarVaga(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.buscarVaga(id));
    }

    @PatchMapping("/vagas/{id}/status/{statusId}")
    @Operation(summary = "Alterar status de uma vaga")
    public ResponseEntity<VagaResponseDTO> alterarStatusVaga(
            @PathVariable Long id,
            @PathVariable Long statusId) {
        return ResponseEntity.ok(adminService.alterarStatusVaga(id, statusId));
    }

    @DeleteMapping("/vagas/{id}")
    @Operation(summary = "Excluir vaga do sistema")
    public ResponseEntity<Void> excluirVaga(@PathVariable Long id) {
        adminService.excluirVaga(id);
        return ResponseEntity.noContent().build();
    }

    // ═══════════════════════════════════════════════════════════
    //  CANDIDATURAS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/candidaturas")
    @Operation(summary = "Listar todas as candidaturas do sistema")
    public ResponseEntity<List<CandidaturaResponseDTO>> listarCandidaturas() {
        return ResponseEntity.ok(adminService.listarCandidaturas());
    }

    @GetMapping("/candidaturas/{id}")
    @Operation(summary = "Buscar candidatura por ID")
    public ResponseEntity<CandidaturaResponseDTO> buscarCandidatura(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.buscarCandidatura(id));
    }

    @PatchMapping("/candidaturas/{id}/status/{statusId}")
    @Operation(summary = "Alterar status de uma candidatura")
    public ResponseEntity<CandidaturaResponseDTO> alterarStatusCandidatura(
            @PathVariable Long id,
            @PathVariable Long statusId) {
        return ResponseEntity.ok(adminService.alterarStatusCandidatura(id, statusId));
    }

    @DeleteMapping("/candidaturas/{id}")
    @Operation(summary = "Excluir candidatura do sistema")
    public ResponseEntity<Void> excluirCandidatura(@PathVariable Long id) {
        adminService.excluirCandidatura(id);
        return ResponseEntity.noContent().build();
    }

    // ═══════════════════════════════════════════════════════════
    //  ADMINISTRADORES
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/admins")
    @Operation(summary = "Listar todos os administradores")
    public ResponseEntity<List<AdministradorResponseDTO>> listarAdmins() {
        return ResponseEntity.ok(adminService.listarAdmins());
    }

    @GetMapping("/admins/{id}")
    @Operation(summary = "Buscar administrador por ID")
    public ResponseEntity<AdministradorResponseDTO> buscarAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.buscarAdmin(id));
    }

    @PostMapping("/admins")
    @Operation(summary = "Promover usuário a administrador")
    public ResponseEntity<AdministradorResponseDTO> criarAdmin(
            @Valid @RequestBody AdministradorRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.criarAdmin(dto));
    }

    @PutMapping("/admins/{id}")
    @Operation(summary = "Atualizar dados do administrador (nível, departamento, permissões)")
    public ResponseEntity<AdministradorResponseDTO> atualizarAdmin(
            @PathVariable Long id,
            @Valid @RequestBody AdministradorRequestDTO dto) {
        return ResponseEntity.ok(adminService.atualizarAdmin(id, dto));
    }

    @DeleteMapping("/admins/{id}")
    @Operation(summary = "Remover administrador (reverte usuário para CANDIDATO)")
    public ResponseEntity<Void> removerAdmin(@PathVariable Long id) {
        adminService.removerAdmin(id);
        return ResponseEntity.noContent().build();
    }

    // ═══════════════════════════════════════════════════════════
    //  ESTATÍSTICAS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/estatisticas")
    @Operation(summary = "Obter estatísticas gerais do sistema")
    public ResponseEntity<EstatisticasSistemaDTO> estatisticas() {
        return ResponseEntity.ok(adminService.estatisticas());
    }
}
