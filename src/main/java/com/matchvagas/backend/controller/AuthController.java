package com.matchvagas.backend.controller;

import com.matchvagas.backend.dto.AuthResponse;
import com.matchvagas.backend.dto.ConfirmarEmailRequestDTO;
import com.matchvagas.backend.dto.EsqueceuSenhaRequestDTO;
import com.matchvagas.backend.dto.LoginRequestDTO;
import com.matchvagas.backend.dto.RedefinirSenhaRequestDTO;
import com.matchvagas.backend.dto.RefreshRequestDTO;
import com.matchvagas.backend.dto.RegisterEmpresaRequestDTO;
import com.matchvagas.backend.dto.ReenviarVerificacaoRequestDTO;
import com.matchvagas.backend.dto.UsuarioResponseDTO;
import com.matchvagas.backend.dto.UsuariosRequestDTO;
import com.matchvagas.backend.dto.VerificarCodigoRequestDTO;
import com.matchvagas.backend.dto.VerificarCodigoResponseDTO;
import com.matchvagas.backend.service.AuthService;
import com.matchvagas.backend.service.EmailVerificationService;
import com.matchvagas.backend.service.PasswordResetService;
import com.matchvagas.backend.service.RateLimiterService;
import com.matchvagas.backend.util.ClientIpResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
// CORS é controlado de forma centralizada no SecurityConfig (SEC-08) — não usar @CrossOrigin aqui
@Tag(name = "Autenticação", description = "Cadastro e login de usuários (RF001, RF002)")
// Endpoints de auth são públicos — remove o esquema JWT global neste controller
@SecurityRequirement(name = "")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;
    private final RateLimiterService rateLimiterService;
    private final ClientIpResolver clientIpResolver;

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
    public ResponseEntity<UsuarioResponseDTO> register(@Valid @RequestBody UsuariosRequestDTO request,
                                                       HttpServletRequest http) {
        rateLimiterService.verificar("register:" + clientIp(http));
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
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequestDTO request,
                                              HttpServletRequest http) {
        rateLimiterService.verificar("login:" + clientIp(http));
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
    public ResponseEntity<AuthResponse> registerEmpresa(@Valid @RequestBody RegisterEmpresaRequestDTO request,
                                                        HttpServletRequest http) {
        rateLimiterService.verificar("register-empresa:" + clientIp(http));
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerEmpresa(request));
    }

    @PostMapping("/confirmar-email")
    @Operation(
        summary = "Confirmar e-mail",
        description = "Valida o token recebido no e-mail de cadastro e ativa o login da conta."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "E-mail confirmado — login liberado"),
        @ApiResponse(responseCode = "400", description = "Token inválido, expirado ou já utilizado",
                     content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<Void> confirmarEmail(@Valid @RequestBody ConfirmarEmailRequestDTO request) {
        emailVerificationService.confirmar(request.token());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reenviar-verificacao")
    @Operation(
        summary = "Reenviar e-mail de verificação",
        description = "Reenvia o link de confirmação. Sempre retorna 200 para não expor quais e-mails existem."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Solicitação processada — se aplicável, um novo link é enviado")
    })
    public ResponseEntity<Void> reenviarVerificacao(@Valid @RequestBody ReenviarVerificacaoRequestDTO request,
                                                    HttpServletRequest http) {
        rateLimiterService.verificar("reenviar-verificacao:" + clientIp(http));
        emailVerificationService.reenviar(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/esqueceu-senha")
    @Operation(
        summary = "Solicitar redefinição de senha",
        description = "Envia um e-mail com link para redefinição de senha. Sempre retorna 200 para não expor quais e-mails estão cadastrados."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Solicitação processada — se o e-mail existir, um link será enviado")
    })
    public ResponseEntity<Void> esqueceuSenha(@Valid @RequestBody EsqueceuSenhaRequestDTO request,
                                              HttpServletRequest http) {
        rateLimiterService.verificar("esqueceu-senha:" + clientIp(http));
        passwordResetService.solicitarRedefinicao(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verificar-codigo")
    @Operation(
        summary = "Verificar código de redefinição",
        description = "Valida o código de 6 dígitos recebido por e-mail e retorna um token para redefinir a senha."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Código válido — token retornado"),
        @ApiResponse(responseCode = "400", description = "Código inválido ou expirado",
                     content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<VerificarCodigoResponseDTO> verificarCodigo(@Valid @RequestBody VerificarCodigoRequestDTO request,
                                                                      HttpServletRequest http) {
        // SEC: limita tentativas por IP — complementa o limite por token no service.
        rateLimiterService.verificar("verificar-codigo:" + clientIp(http));
        String token = passwordResetService.verificarCodigo(request.email(), request.codigo());
        return ResponseEntity.ok(new VerificarCodigoResponseDTO(token));
    }

    @PostMapping("/redefinir-senha")
    @Operation(
        summary = "Redefinir senha com token",
        description = "Usa o token obtido em /verificar-codigo para definir a nova senha. O token expira em 1 hora e só pode ser usado uma vez."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Senha redefinida com sucesso"),
        @ApiResponse(responseCode = "400", description = "Token inválido, expirado ou já utilizado",
                     content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<Void> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequestDTO request,
                                               HttpServletRequest http) {
        rateLimiterService.verificar("redefinir-senha:" + clientIp(http));
        passwordResetService.redefinirSenha(request.token(), request.novaSenha());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Renovar access token",
        description = "Troca um refresh token válido por um novo par de tokens. O refresh token é rotacionado (o antigo é invalidado)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Novo access token e refresh token retornados"),
        @ApiResponse(responseCode = "400", description = "Refresh token inválido, expirado ou revogado",
                     content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequestDTO request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Logout",
        description = "Revoga o refresh token no servidor, encerrando a sessão. O cliente deve descartar o access token."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sessão encerrada")
    })
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequestDTO request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok().build();
    }

    /**
     * IP de origem do cliente, resistente a falsificação de X-Forwarded-For
     * (SEC-06). Delega ao {@link ClientIpResolver}, que considera apenas os hops
     * dos proxies confiáveis configurados.
     */
    private String clientIp(HttpServletRequest request) {
        return clientIpResolver.resolve(request);
    }
}
