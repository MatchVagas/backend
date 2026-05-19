package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.AuthResponse;
import com.matchvagas.backend.dto.EsqueceuSenhaRequestDTO;
import com.matchvagas.backend.dto.LoginRequestDTO;
import com.matchvagas.backend.dto.RedefinirSenhaRequestDTO;
import com.matchvagas.backend.dto.RegisterEmpresaRequestDTO;
import com.matchvagas.backend.dto.UsuarioResponseDTO;
import com.matchvagas.backend.dto.UsuariosRequestDTO;
import com.matchvagas.backend.service.AuthService;
import com.matchvagas.backend.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Autenticação", description = "Cadastro e login de usuários (RF001, RF002)")
// Endpoints de auth são públicos — remove o esquema JWT global neste controller
@SecurityRequirement(name = "")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    @Operation(
        summary = "Cadastrar novo usuário",
        description = "Cria um novo usuário do tipo CANDIDATO, EMPRESA ou ADMIN. Retorna os dados do usuário criado (sem a senha)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Email já cadastrado ou dados inválidos",
                     content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<UsuarioResponseDTO> register(@Valid @RequestBody UsuariosRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(
        summary = "Fazer login",
        description = "Autentica o usuário e retorna um token JWT. Use o token no botão **Authorize** (🔒) acima para liberar os demais endpoints."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login realizado — token JWT retornado"),
        @ApiResponse(responseCode = "400", description = "Email ou senha inválidos",
                     content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                     content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register-empresa")
    @Operation(
        summary = "Cadastrar empresa (usuário + empresa em uma única operação atômica)",
        description = "Cria o usuário e a empresa numa única transação. Se qualquer etapa falhar, nada é persistido."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Empresa cadastrada — token JWT retornado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos, CNPJ inválido ou e-mail já em uso",
                     content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<AuthResponse> registerEmpresa(@Valid @RequestBody RegisterEmpresaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerEmpresa(request));
    }

    @PostMapping("/esqueceu-senha")
    @Operation(
        summary = "Solicitar redefinição de senha",
        description = "Envia um e-mail com link para redefinição de senha. Sempre retorna 200 para não expor quais e-mails estão cadastrados."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Solicitação processada — se o e-mail existir, um link será enviado")
    })
    public ResponseEntity<Void> esqueceuSenha(@Valid @RequestBody EsqueceuSenhaRequestDTO request) {
        passwordResetService.solicitarRedefinicao(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/redefinir-senha")
    @Operation(
        summary = "Redefinir senha com token",
        description = "Valida o token recebido por e-mail e define a nova senha do usuário. O token expira em 1 hora e só pode ser usado uma vez."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Senha redefinida com sucesso"),
        @ApiResponse(responseCode = "400", description = "Token inválido, expirado ou já utilizado",
                     content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<Void> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequestDTO request) {
        passwordResetService.redefinirSenha(request.token(), request.novaSenha());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Logout",
        description = "Com JWT stateless o logout é feito removendo o token no cliente. Este endpoint existe apenas para documentação."
    )
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().build();
    }
}
