package com.matchvagas.backend.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Matriz de autorização de todos os endpoints da API, contra o contexto real (SecurityConfig +
 * {@code @PreAuthorize}) sobre H2. Para cada endpoint e perfil: quem não tem acesso recebe 401/403;
 * quem tem acesso passa pela segurança (ids inexistentes: nunca altera dados de outros testes) e não recebe erro 5xx (o banco vazio leva a 400/404/409).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Integração — matriz de autorização dos endpoints")
class MatrizAutorizacaoIntegrationTest {

    enum Perfil { ANONIMO, CANDIDATO, EMPRESA, ADMIN }

    private static final Set<Perfil> PUBLICO = EnumSet.allOf(Perfil.class);
    private static final Set<Perfil> AUTENTICADO = EnumSet.of(Perfil.CANDIDATO, Perfil.EMPRESA, Perfil.ADMIN);
    private static final Set<Perfil> CANDIDATO = EnumSet.of(Perfil.CANDIDATO);
    private static final Set<Perfil> EMPRESA = EnumSet.of(Perfil.EMPRESA);
    private static final Set<Perfil> ADMIN = EnumSet.of(Perfil.ADMIN);
    private static final Set<Perfil> EMPRESA_ADMIN = EnumSet.of(Perfil.EMPRESA, Perfil.ADMIN);
    private static final Set<Perfil> CANDIDATO_EMPRESA = EnumSet.of(Perfil.CANDIDATO, Perfil.EMPRESA);

    @Autowired MockMvc mockMvc;

    private static Arguments e(String metodo, String caminho, Set<Perfil> permitidos) {
        return Arguments.of(metodo, caminho, permitidos);
    }

    static Stream<Arguments> endpoints() {
        Stream.Builder<Arguments> s = Stream.builder();

        // ── Autenticação (pública) ───────────────────────────────────────────
        for (String p : new String[]{"register", "login", "register-empresa", "confirmar-email",
                "reenviar-verificacao", "esqueceu-senha", "verificar-codigo", "redefinir-senha", "refresh", "logout"}) {
            s.add(e("POST", "/api/auth/" + p, PUBLICO));
        }

        // ── Vagas ────────────────────────────────────────────────────────────
        s.add(e("GET", "/api/vagas", PUBLICO));
        s.add(e("GET", "/api/vagas/busca", PUBLICO));
        s.add(e("GET", "/api/vagas/999999", PUBLICO));
        s.add(e("GET", "/api/vagas/empresa/999999", PUBLICO));
        s.add(e("GET", "/api/vagas/minhas", EMPRESA));
        s.add(e("GET", "/api/vagas/999999/candidatos-recomendados", EMPRESA));
        s.add(e("POST", "/api/vagas/assistente/descricao", EMPRESA_ADMIN));
        s.add(e("POST", "/api/vagas", EMPRESA_ADMIN));
        s.add(e("PUT", "/api/vagas/999999", EMPRESA_ADMIN));
        s.add(e("DELETE", "/api/vagas/999999", EMPRESA_ADMIN));

        // ── Empresas ─────────────────────────────────────────────────────────
        s.add(e("GET", "/api/empresas", PUBLICO));
        s.add(e("GET", "/api/empresas/999999", PUBLICO));
        s.add(e("GET", "/api/empresas/minha-empresa", EMPRESA));
        s.add(e("POST", "/api/empresas/minha-empresa/logo", EMPRESA));
        s.add(e("PUT", "/api/empresas/minha-empresa/logo", EMPRESA));
        s.add(e("DELETE", "/api/empresas/minha-empresa/logo", EMPRESA));
        s.add(e("POST", "/api/empresas/talentos/busca", EMPRESA));
        s.add(e("POST", "/api/empresas", EMPRESA_ADMIN));
        s.add(e("PUT", "/api/empresas/999999", EMPRESA_ADMIN));

        // ── Candidato: perfil, currículo, experiências, formações, habilidades ──
        s.add(e("GET", "/api/candidatos/meu-perfil", CANDIDATO));
        s.add(e("POST", "/api/candidatos", CANDIDATO));
        s.add(e("PUT", "/api/candidatos/meu-perfil", CANDIDATO));
        s.add(e("POST", "/api/candidatos/meu-perfil/foto", CANDIDATO));
        s.add(e("PUT", "/api/candidatos/meu-perfil/foto", CANDIDATO));
        s.add(e("DELETE", "/api/candidatos/meu-perfil/foto", CANDIDATO));
        s.add(e("GET", "/api/candidatos/meus-dados", CANDIDATO));
        s.add(e("GET", "/api/candidatos/sugestoes", CANDIDATO));
        s.add(e("PATCH", "/api/candidatos/recomendacoes/visibilidade", CANDIDATO));
        s.add(e("GET", "/api/candidatos/recomendacoes/visibilidade", CANDIDATO));
        s.add(e("POST", "/api/candidatos/curriculo/upload", CANDIDATO));
        s.add(e("GET", "/api/candidatos/curriculo/download", CANDIDATO));
        s.add(e("GET", "/api/candidatos/curriculo", CANDIDATO));
        s.add(e("DELETE", "/api/candidatos/curriculo", CANDIDATO));
        s.add(e("POST", "/api/candidatos/curriculo/resumo", CANDIDATO));
        s.add(e("GET", "/api/candidatos/curriculo/resumo", CANDIDATO));
        for (String recurso : new String[]{"experiencias", "formacoes"}) {
            s.add(e("GET", "/api/candidatos/" + recurso, CANDIDATO));
            s.add(e("POST", "/api/candidatos/" + recurso, CANDIDATO));
            s.add(e("PUT", "/api/candidatos/" + recurso + "/999999", CANDIDATO));
            s.add(e("DELETE", "/api/candidatos/" + recurso + "/999999", CANDIDATO));
        }
        s.add(e("GET", "/api/candidatos/habilidades", CANDIDATO));
        s.add(e("POST", "/api/candidatos/habilidades", CANDIDATO));
        s.add(e("PUT", "/api/candidatos/habilidades/Java", CANDIDATO));
        s.add(e("DELETE", "/api/candidatos/habilidades/Java", CANDIDATO));

        // ── Candidaturas ─────────────────────────────────────────────────────
        s.add(e("POST", "/api/candidaturas", CANDIDATO));
        s.add(e("GET", "/api/candidaturas/minhas", CANDIDATO));
        s.add(e("GET", "/api/candidaturas/999999", CANDIDATO));
        s.add(e("PATCH", "/api/candidaturas/999999/compartilhamento", CANDIDATO));
        s.add(e("GET", "/api/candidaturas/empresa", EMPRESA));
        s.add(e("GET", "/api/candidaturas/empresa/vaga/999999", EMPRESA));
        s.add(e("GET", "/api/candidaturas/empresa/vaga/999999/kanban", EMPRESA));
        s.add(e("GET", "/api/candidaturas/999999/empresa", EMPRESA));
        s.add(e("GET", "/api/candidaturas/999999/curriculo/download", EMPRESA));
        s.add(e("PATCH", "/api/candidaturas/999999/empresa/status/999999", EMPRESA));
        s.add(e("GET", "/api/candidaturas/999999/empresa/historico", EMPRESA));
        s.add(e("POST", "/api/candidaturas/999999/empresa/triagem", EMPRESA));
        s.add(e("GET", "/api/candidaturas/999999/empresa/triagem", EMPRESA));

        // ── Favoritos e alertas ──────────────────────────────────────────────
        s.add(e("POST", "/api/favoritos/999999", CANDIDATO));
        s.add(e("DELETE", "/api/favoritos/999999", CANDIDATO));
        s.add(e("GET", "/api/favoritos", CANDIDATO));
        s.add(e("GET", "/api/favoritos/999999", CANDIDATO));
        s.add(e("POST", "/api/alertas", CANDIDATO));
        s.add(e("GET", "/api/alertas", CANDIDATO));
        s.add(e("PATCH", "/api/alertas/999999", CANDIDATO));
        s.add(e("DELETE", "/api/alertas/999999", CANDIDATO));

        // ── Mensagens e notificações ─────────────────────────────────────────
        s.add(e("POST", "/api/mensagens", CANDIDATO_EMPRESA));
        s.add(e("GET", "/api/mensagens/candidatura/999999", CANDIDATO_EMPRESA));
        s.add(e("GET", "/api/mensagens/candidatura/999999/nao-lidas/contagem", CANDIDATO_EMPRESA));
        s.add(e("GET", "/api/mensagens/nao-lidas/contagem", CANDIDATO_EMPRESA));
        s.add(e("PATCH", "/api/mensagens/candidatura/999999/lidas", CANDIDATO_EMPRESA));
        s.add(e("POST", "/api/notificacoes", ADMIN));
        s.add(e("GET", "/api/notificacoes/minhas", AUTENTICADO));
        s.add(e("GET", "/api/notificacoes/minhas/nao-lidas", AUTENTICADO));
        s.add(e("GET", "/api/notificacoes/minhas/contagem", AUTENTICADO));
        s.add(e("PATCH", "/api/notificacoes/999999/lida", AUTENTICADO));
        s.add(e("PATCH", "/api/notificacoes/lidas/todas", AUTENTICADO));
        s.add(e("DELETE", "/api/notificacoes/999999", AUTENTICADO));

        // ── Localização e lookups: leitura pública, escrita ADMIN ────────────
        Stream.of("/api/localizacao/paises", "/api/localizacao/estados", "/api/localizacao/cidades",
                "/api/lookup/notificacoes/tipos", "/api/lookup/sistema/status-candidatura",
                "/api/lookup/sistema/tipos-telefone", "/api/lookup/sistema/departamentos",
                "/api/lookup/vagas/tipos", "/api/lookup/vagas/modalidades", "/api/lookup/vagas/escolaridades",
                "/api/lookup/vagas/portes", "/api/lookup/vagas/ramos", "/api/lookup/vagas/status")
                .forEach(base -> {
                    s.add(e("GET", base, PUBLICO));
                    s.add(e("POST", base, ADMIN));
                    s.add(e("PUT", base + "/999999", ADMIN));
                    s.add(e("DELETE", base + "/999999", ADMIN));
                });
        Stream.of("/api/localizacao/paises/999999", "/api/localizacao/estados/999999", "/api/localizacao/cidades/999999")
                .forEach(p -> s.add(e("GET", p, PUBLICO)));

        // ── Usuários (ADMIN) ─────────────────────────────────────────────────
        s.add(e("GET", "/api/usuarios", ADMIN));
        s.add(e("GET", "/api/usuarios/999999", ADMIN));
        s.add(e("GET", "/api/usuarios/email/a@x.com", ADMIN));
        s.add(e("POST", "/api/usuarios", ADMIN));
        s.add(e("PUT", "/api/usuarios/999999", ADMIN));
        s.add(e("DELETE", "/api/usuarios/999999", ADMIN));

        // ── Administração ────────────────────────────────────────────────────
        Stream.of(
                "GET /api/admin/usuarios", "GET /api/admin/usuarios/999999", "PUT /api/admin/usuarios/999999",
                "GET /api/admin/usuarios/empresa/pendentes", "PATCH /api/admin/usuarios/999999/aprovar",
                "DELETE /api/admin/usuarios/999999/rejeitar", "PATCH /api/admin/usuarios/999999/ativar",
                "PATCH /api/admin/usuarios/999999/desativar", "PATCH /api/admin/usuarios/999999/tipo/EMPRESA",
                "DELETE /api/admin/usuarios/999999",
                "GET /api/admin/candidatos", "GET /api/admin/candidatos/999999", "PUT /api/admin/candidatos/999999",
                "DELETE /api/admin/candidatos/999999",
                "GET /api/admin/empresas", "GET /api/admin/empresas/pendentes", "GET /api/admin/empresas/999999",
                "PATCH /api/admin/empresas/999999/aprovar", "PATCH /api/admin/empresas/999999/rejeitar",
                "DELETE /api/admin/empresas/999999",
                "GET /api/admin/vagas", "GET /api/admin/vagas/999999", "PATCH /api/admin/vagas/999999/status/999999",
                "DELETE /api/admin/vagas/999999",
                "GET /api/admin/candidaturas", "GET /api/admin/candidaturas/999999",
                "PATCH /api/admin/candidaturas/999999/status/999999", "DELETE /api/admin/candidaturas/999999",
                "GET /api/admin/admins", "GET /api/admin/admins/999999", "POST /api/admin/admins",
                "PUT /api/admin/admins/999999", "DELETE /api/admin/admins/999999",
                "GET /api/admin/estatisticas",
                "POST /api/admin/migracao/backfill-cpf", "POST /api/admin/migracao/normalizar-urls-imagens",
                "POST /api/admin/migracao/backfill-embeddings",
                "GET /actuator/metrics", "GET /actuator/info")
                .map(l -> l.split(" "))
                .forEach(p -> s.add(e(p[0], p[1], ADMIN)));
        s.add(e("GET", "/actuator/health", PUBLICO));

        return s.build();
    }

    private MockHttpServletRequestBuilder requisicao(String metodo, String caminho, Perfil perfil) {
        MockHttpServletRequestBuilder req = request(HttpMethod.valueOf(metodo), caminho);
        if (!metodo.equals("GET") && !metodo.equals("DELETE")) {
            req.contentType(MediaType.APPLICATION_JSON).content("{}");
        }
        if (perfil != Perfil.ANONIMO) {
            req.with(user("999999").authorities(new SimpleGrantedAuthority(perfil.name())));
        }
        return req;
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("endpoints")
    @DisplayName("acesso por perfil")
    void acessoPorPerfil(String metodo, String caminho, Set<Perfil> permitidos) throws Exception {
        for (Perfil perfil : Perfil.values()) {
            // Endpoints públicos são verificados só como anônimo: evita gastar a cota de rate limit.
            if (permitidos == PUBLICO && perfil != Perfil.ANONIMO) continue;

            var resposta = mockMvc.perform(requisicao(metodo, caminho, perfil)).andReturn().getResponse();
            int status = resposta.getStatus();

            if (permitidos.contains(perfil)) {
                assertThat(status)
                        .as("%s %s como %s deveria passar pela segurança sem erro 5xx", metodo, caminho, perfil)
                        .isNotIn(401, 403)
                        .isLessThan(500);
            } else if (status == 400 || status == 415) {
                // @PreAuthorize roda depois da leitura da requisição (corpo @Valid, parâmetros,
                // Content-Type): o "{}" pode ser recusado antes do bloqueio. Aceito só se o serviço
                // não rodou — um erro de regra de negócio indicaria que a chamada passou.
                assertThat(resposta.getContentAsString())
                        .as("%s %s como %s: recusa antes do bloqueio não pode vir do serviço", metodo, caminho, perfil)
                        .doesNotContain("Erro na regra de negócio");
            } else {
                assertThat(status)
                        .as("%s %s como %s deveria ser bloqueado", metodo, caminho, perfil)
                        .isIn(401, 403);
            }
        }
    }
}
