package com.matchvagas.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    // ── Configuração global da API ────────────────────────────────────────────

    @Bean
    public OpenAPI matchVagasOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MatchVagas API")
                        .description("""
                                API REST do sistema **MatchVagas** — plataforma de banco de vagas de trabalho e estágio.
                                
                                ## Autenticação
                                A maioria dos endpoints requer um token JWT. Para obtê-lo:
                                1. Cadastre um usuário em `POST /api/auth/register`
                                2. Faça login em `POST /api/auth/login`
                                3. Copie o campo `token` da resposta
                                4. Clique em **Authorize** (🔒) acima e cole: `Bearer {seu_token}`
                                
                                ## Perfis de acesso
                                | Perfil | Acesso |
                                |--------|--------|
                                | **Público** | Listagem de vagas, empresas e dados auxiliares |
                                | **CANDIDATO** | Gerenciar perfil, candidatar-se e acompanhar candidaturas |
                                | **EMPRESA** | Publicar e gerenciar vagas |
                                | **ADMIN** | Gestão global do sistema |
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Equipe MatchVagas")
                                .email("contato@matchvagas.com.br")
                                .url("https://github.com/MatchVagas/backend"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))

                // Servidores disponíveis no dropdown do Swagger UI
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Desenvolvimento local"),
                        new Server().url("https://backend-tgi8.onrender.com/").description("Produção")
                ))

                // Esquema de autenticação JWT — aparece no botão Authorize
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Cole aqui o token JWT obtido no endpoint /api/auth/login")))

                // Aplica o esquema JWT globalmente em todos os endpoints
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }

    // ── Grupos — aparecem como abas separadas no Swagger UI ──────────────────

    @Bean
    public GroupedOpenApi grupoAuth() {
        return GroupedOpenApi.builder()
                .group("1 — Autenticação")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi grupoCandidato() {
        return GroupedOpenApi.builder()
                .group("2 — Candidato")
                .pathsToMatch("/api/candidatos/**", "/api/candidaturas/**")
                .build();
    }

    @Bean
    public GroupedOpenApi grupoEmpresa() {
        return GroupedOpenApi.builder()
                .group("3 — Empresa")
                .pathsToMatch("/api/empresas/**")
                .build();
    }

    @Bean
    public GroupedOpenApi grupoVagas() {
        return GroupedOpenApi.builder()
                .group("4 — Vagas")
                .pathsToMatch("/api/vagas/**")
                .build();
    }

    @Bean
    public GroupedOpenApi grupoLocalizacao() {
        return GroupedOpenApi.builder()
                .group("5 — Localização")
                .pathsToMatch("/api/localizacao/**")
                .build();
    }

    @Bean
    public GroupedOpenApi grupoLookup() {
        return GroupedOpenApi.builder()
                .group("6 — Lookup (dados auxiliares)")
                .pathsToMatch("/api/lookup/**")
                .build();
    }

    @Bean
    public GroupedOpenApi grupoAdmin() {
        return GroupedOpenApi.builder()
                .group("7 — Administração")
                .pathsToMatch("/api/admin/**", "/api/usuarios/**")
                .build();
    }

    @Bean
    public GroupedOpenApi grupoCompleto() {
        return GroupedOpenApi.builder()
                .group("0 — Todos os endpoints")
                .pathsToMatch("/api/**")
                .build();
    }
}
