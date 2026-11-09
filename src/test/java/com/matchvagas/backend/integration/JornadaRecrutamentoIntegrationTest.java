package com.matchvagas.backend.integration;

import com.jayway.jsonpath.JsonPath;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.entity.Usuarios.TipoUsuario;
import com.matchvagas.backend.repository.EmailVerificationTokenRepository;
import com.matchvagas.backend.repository.UsuariosRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Jornada de recrutamento de ponta a ponta pela API, com JWT real, segurança real e banco H2:
 * admin configura os cadastros → candidato se cadastra, confirma o e-mail e monta o perfil →
 * empresa se cadastra e é aprovada → publica vaga → encontra o candidato → candidatura →
 * mudança de status com notificação → chat → favoritos → renovação e encerramento da sessão.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Integração — jornada completa de recrutamento")
class JornadaRecrutamentoIntegrationTest {

    private static final String SENHA = "Senha@123";
    // Cada chamada de autenticação sai de um IP diferente para não esbarrar no rate limit por IP.
    private static final AtomicInteger IP = new AtomicInteger(1);

    @Autowired MockMvc mockMvc;
    @Autowired UsuariosRepository usuariosRepository;
    @Autowired EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private final String sufixo = UUID.randomUUID().toString().substring(0, 8);
    private final String emailCandidato = "cand-" + sufixo + "@teste.com";
    private final String emailEmpresa = "emp-" + sufixo + "@teste.com";

    private String tokenAdmin, tokenCandidato, tokenEmpresa, refreshCandidato;
    private long tipoVagaId, modalidadeId, escolaridadeId, porteId, ramoId, statusVagaId, cidadeId, statusAprovadoId;
    private long empresaId, vagaId, candidaturaId;

    // ── utilitários ──────────────────────────────────────────────────────────

    private ResultActions chamar(HttpMethod metodo, String caminho, String token, String json) throws Exception {
        MockHttpServletRequestBuilder req = request(metodo, caminho)
                .with(r -> { r.setRemoteAddr("10.9." + (IP.get() / 250) + "." + (IP.getAndIncrement() % 250)); return r; });
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (json != null) req.contentType(MediaType.APPLICATION_JSON).content(json);
        return mockMvc.perform(req);
    }

    private String corpo(ResultActions resultado) throws Exception {
        return resultado.andReturn().getResponse().getContentAsString();
    }

    private long id(ResultActions resultado) throws Exception {
        return ((Number) JsonPath.read(corpo(resultado), "$.id")).longValue();
    }

    private String login(String email) throws Exception {
        return JsonPath.read(corpo(chamar(HttpMethod.POST, "/api/auth/login", null,
                "{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}").andExpect(status().isOk())), "$.token");
    }

    /** Confirma o e-mail pelo endpoint público, com o token que iria no link do e-mail. */
    private void confirmarEmail(String email) throws Exception {
        Long usuarioId = usuariosRepository.findByEmail(email).orElseThrow().getId();
        String tokenEmail = emailVerificationTokenRepository.findAll().stream()
                .filter(t -> t.getUsuario().getId().equals(usuarioId))
                .findFirst().orElseThrow().getToken();
        chamar(HttpMethod.POST, "/api/auth/confirmar-email", null, "{\"token\":\"" + tokenEmail + "\"}")
                .andExpect(status().is2xxSuccessful());
    }

    private long criarLookup(String caminho, String json) throws Exception {
        return id(chamar(HttpMethod.POST, caminho, tokenAdmin, json).andExpect(status().is2xxSuccessful()));
    }

    // ── jornada ──────────────────────────────────────────────────────────────

    @BeforeAll
    void criarAdmin() throws Exception {
        Usuarios admin = new Usuarios();
        admin.setNome("Admin");
        admin.setEmail("admin-" + sufixo + "@teste.com");
        admin.setSenha(passwordEncoder.encode(SENHA));
        admin.setTipoUsuario(TipoUsuario.ADMIN);
        admin.setAtivo(true);
        admin.setEmailVerificado(true);
        admin.setDataUltimoAcesso(LocalDateTime.now());
        admin.setDataNascimento(java.sql.Date.valueOf("1990-01-01"));
        admin.setIdade(36);
        usuariosRepository.save(admin);
        tokenAdmin = login(admin.getEmail());
    }

    @Test
    @Order(1)
    @DisplayName("admin cadastra os dados de apoio (lookups e localização)")
    void adminCadastraLookups() throws Exception {
        tipoVagaId = criarLookup("/api/lookup/vagas/tipos", "{\"descricao\":\"CLT\"}");
        modalidadeId = criarLookup("/api/lookup/vagas/modalidades", "{\"descricao\":\"Remoto\"}");
        escolaridadeId = criarLookup("/api/lookup/vagas/escolaridades", "{\"nome\":\"Ensino Superior\",\"ordem\":4}");
        porteId = criarLookup("/api/lookup/vagas/portes", "{\"descricao\":\"Média\"}");
        ramoId = criarLookup("/api/lookup/vagas/ramos", "{\"descricao\":\"Tecnologia\"}");
        statusVagaId = criarLookup("/api/lookup/vagas/status", "{\"descricao\":\"Ativa\"}");
        statusAprovadoId = criarLookup("/api/lookup/sistema/status-candidatura", "{\"status\":\"Aprovado\"}");
        criarLookup("/api/lookup/notificacoes/tipos", "{\"status\":\"Candidatura\"}");
        long paisId = criarLookup("/api/localizacao/paises", "{\"nome\":\"Brasil\",\"codigoIso\":\"BR\"}");
        long estadoId = criarLookup("/api/localizacao/estados",
                "{\"nome\":\"São Paulo\",\"uf\":\"SP\",\"paisId\":" + paisId + "}");
        cidadeId = criarLookup("/api/localizacao/cidades", "{\"nome\":\"São Paulo\",\"estadoId\":" + estadoId + "}");

        chamar(HttpMethod.GET, "/api/lookup/vagas/modalidades", null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].descricao", hasItem("Remoto")));
    }

    @Test
    @Order(2)
    @DisplayName("candidato se cadastra; o login só libera depois de confirmar o e-mail")
    void candidatoCadastraEConfirmaEmail() throws Exception {
        chamar(HttpMethod.POST, "/api/auth/register", null, """
                {"nome":"Ana Souza","email":"%s","senha":"%s","aceitouTermos":true,"dataNascimento":"1995-05-10T00:00:00",
                 "tipoUsuario":"ADMIN"}""".formatted(emailCandidato, SENHA))
                .andExpect(status().is2xxSuccessful());

        // O cadastro público ignora o papel pedido: nasce sempre CANDIDATO.
        assertThat(usuariosRepository.findByEmail(emailCandidato).orElseThrow().getTipoUsuario())
                .isEqualTo(TipoUsuario.CANDIDATO);

        chamar(HttpMethod.POST, "/api/auth/login", null,
                "{\"email\":\"" + emailCandidato + "\",\"senha\":\"" + SENHA + "\"}")
                .andExpect(status().isUnauthorized());

        confirmarEmail(emailCandidato);

        String resposta = corpo(chamar(HttpMethod.POST, "/api/auth/login", null,
                "{\"email\":\"" + emailCandidato + "\",\"senha\":\"" + SENHA + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("CANDIDATO")));
        tokenCandidato = JsonPath.read(resposta, "$.token");
        refreshCandidato = JsonPath.read(resposta, "$.refreshToken");
    }

    @Test
    @Order(3)
    @DisplayName("senha errada não autentica")
    void senhaErrada() throws Exception {
        chamar(HttpMethod.POST, "/api/auth/login", null,
                "{\"email\":\"" + emailCandidato + "\",\"senha\":\"errada123\"}")
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(4)
    @DisplayName("candidato monta o perfil: dados, habilidade, experiência e formação")
    void candidatoMontaPerfil() throws Exception {
        chamar(HttpMethod.POST, "/api/candidatos", tokenCandidato, """
                {"cpf":"529.982.247-25","resumoProfissional":"Desenvolvedora Java backend com Spring",
                 "disponibilidade":"Imediata","pretensaoSalarial":7000}""")
                .andExpect(status().is2xxSuccessful());
        chamar(HttpMethod.POST, "/api/candidatos/habilidades", tokenCandidato,
                "{\"nome\":\"Java\",\"nivel\":\"AVANCADO\"}")
                .andExpect(status().is2xxSuccessful());
        chamar(HttpMethod.POST, "/api/candidatos/habilidades", tokenCandidato, "{\"nome\":\"java\"}")
                .andExpect(status().isBadRequest());
        chamar(HttpMethod.POST, "/api/candidatos/experiencias", tokenCandidato,
                "{\"empresa\":\"ACME\",\"cargo\":\"Dev Java\",\"dataInicio\":\"2021-01\"}")
                .andExpect(status().is2xxSuccessful());
        chamar(HttpMethod.POST, "/api/candidatos/formacoes", tokenCandidato,
                "{\"instituicao\":\"UFX\",\"curso\":\"Computação\",\"nivel\":\"Superior\",\"dataInicio\":\"2016-02\"}")
                .andExpect(status().is2xxSuccessful());

        chamar(HttpMethod.GET, "/api/candidatos/habilidades", tokenCandidato, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome", hasItem("Java")));
        chamar(HttpMethod.GET, "/api/candidatos/meu-perfil", tokenCandidato, null)
                .andExpect(status().isOk());
        chamar(HttpMethod.PATCH, "/api/candidatos/recomendacoes/visibilidade", tokenCandidato,
                "{\"disponivel\":true}")
                .andExpect(status().isOk());
    }

    @Test
    @Order(5)
    @DisplayName("empresa se cadastra (sem data de nascimento), confirma o e-mail e só publica depois de aprovada")
    void empresaCadastraEAprovada() throws Exception {
        chamar(HttpMethod.POST, "/api/auth/register-empresa", null, """
                {"nome":"Carlos RH","email":"%s","senha":"%s","cnpj":"11.222.333/0001-81",
                 "razaoSocial":"Tech LTDA","nomeFantasia":"Tech","porteId":%d,"ramoId":%d,"aceitouTermos":true}"""
                .formatted(emailEmpresa, SENHA, porteId, ramoId))
                .andExpect(status().isCreated())
                // A sessão só começa no login, depois de confirmar o e-mail.
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());

        chamar(HttpMethod.POST, "/api/auth/login", null,
                "{\"email\":\"" + emailEmpresa + "\",\"senha\":\"" + SENHA + "\"}")
                .andExpect(status().isUnauthorized());
        confirmarEmail(emailEmpresa);
        tokenEmpresa = login(emailEmpresa);

        empresaId = id(chamar(HttpMethod.GET, "/api/empresas/minha-empresa", tokenEmpresa, null)
                .andExpect(status().isOk()));

        chamar(HttpMethod.POST, "/api/vagas", tokenEmpresa, vagaJson())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("aprovada")));

        chamar(HttpMethod.PATCH, "/api/admin/empresas/" + empresaId + "/aprovar", tokenAdmin, null)
                .andExpect(status().isOk());
    }

    private String vagaJson() {
        return """
                {"titulo":"Desenvolvedor Java Pleno","descricao":"Desenvolvimento de APIs REST com Spring Boot",
                 "requisitos":"Java, Spring Boot, SQL","areaAtuacao":"Tecnologia Java",
                 "tipoVagaId":%d,"modalidadeId":%d,"escolaridadeId":%d,"statusVagaId":%d,"cidadeId":%d,
                 "empresaId":%d,"cargaHoraria":"40h semanais",
                 "salarioMinimo":6000,"salarioMaximo":9000,"numeroVagas":1,"dataExpiracao":"%s"}"""
                .formatted(tipoVagaId, modalidadeId, escolaridadeId, statusVagaId, cidadeId, empresaId,
                        LocalDateTime.now().plusDays(30).withNano(0));
    }

    @Test
    @Order(6)
    @DisplayName("empresa aprovada publica a vaga, que fica visível publicamente")
    void empresaPublicaVaga() throws Exception {
        vagaId = id(chamar(HttpMethod.POST, "/api/vagas", tokenEmpresa, vagaJson())
                .andExpect(status().is2xxSuccessful()));

        chamar(HttpMethod.GET, "/api/vagas/" + vagaId, null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Desenvolvedor Java Pleno"));
        chamar(HttpMethod.GET, "/api/vagas/minhas", tokenEmpresa, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem((int) vagaId)));
    }

    @Test
    @Order(7)
    @DisplayName("empresa encontra o candidato pela recomendação da vaga e pela busca de talentos")
    void empresaEncontraCandidato() throws Exception {
        chamar(HttpMethod.GET, "/api/vagas/" + vagaId + "/candidatos-recomendados", tokenEmpresa, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
        chamar(HttpMethod.POST, "/api/empresas/talentos/busca", tokenEmpresa,
                "{\"descricao\":\"Desenvolvedor Java backend com Spring\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
                // Minimização: o ranking nunca expõe CPF nem contato.
                .andExpect(jsonPath("$.content[0].cpf").doesNotExist())
                .andExpect(jsonPath("$.content[0].email").doesNotExist());
    }

    @Test
    @Order(8)
    @DisplayName("candidato vê a vaga nas sugestões e se candidata uma única vez")
    void candidatoSeCandidata() throws Exception {
        chamar(HttpMethod.GET, "/api/candidatos/sugestoes", tokenCandidato, null)
                .andExpect(status().isOk());

        candidaturaId = id(chamar(HttpMethod.POST, "/api/candidaturas", tokenCandidato,
                "{\"vagaId\":" + vagaId + "}")
                .andExpect(status().is2xxSuccessful()));
        chamar(HttpMethod.POST, "/api/candidaturas", tokenCandidato, "{\"vagaId\":" + vagaId + "}")
                .andExpect(status().isBadRequest());

        chamar(HttpMethod.GET, "/api/candidaturas/minhas", tokenCandidato, null)
                .andExpect(status().isOk());
    }

    @Test
    @Order(9)
    @DisplayName("empresa vê a candidatura no kanban e aprova; o candidato é notificado")
    void empresaAvaliaCandidatura() throws Exception {
        chamar(HttpMethod.GET, "/api/candidaturas/empresa/vaga/" + vagaId + "/kanban", tokenEmpresa, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCandidaturas").value(1));

        chamar(HttpMethod.PATCH, "/api/candidaturas/" + candidaturaId + "/empresa/status/" + statusAprovadoId,
                tokenEmpresa, null)
                .andExpect(status().isOk());
        chamar(HttpMethod.GET, "/api/candidaturas/" + candidaturaId + "/empresa/historico", tokenEmpresa, null)
                .andExpect(status().isOk());

        chamar(HttpMethod.GET, "/api/notificacoes/minhas/contagem", tokenCandidato, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.naoLidas", greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(10)
    @DisplayName("empresa e candidato conversam pelo chat da candidatura")
    void chat() throws Exception {
        chamar(HttpMethod.POST, "/api/mensagens", tokenEmpresa,
                "{\"candidaturaId\":" + candidaturaId + ",\"conteudo\":\"Podemos marcar uma entrevista?\"}")
                .andExpect(status().is2xxSuccessful());

        chamar(HttpMethod.GET, "/api/mensagens/nao-lidas/contagem", tokenCandidato, null)
                .andExpect(jsonPath("$.naoLidas").value(1));
        chamar(HttpMethod.GET, "/api/mensagens/candidatura/" + candidaturaId, tokenCandidato, null)
                .andExpect(status().isOk());
        chamar(HttpMethod.PATCH, "/api/mensagens/candidatura/" + candidaturaId + "/lidas", tokenCandidato, null)
                .andExpect(status().is2xxSuccessful());
        chamar(HttpMethod.GET, "/api/mensagens/nao-lidas/contagem", tokenCandidato, null)
                .andExpect(jsonPath("$.naoLidas").value(0));
    }

    @Test
    @Order(11)
    @DisplayName("outra empresa não acessa a candidatura nem o chat")
    void isolamentoEntreEmpresas() throws Exception {
        String emailOutra = "outra-" + sufixo + "@teste.com";
        chamar(HttpMethod.POST, "/api/auth/register-empresa", null, """
                {"nome":"Outra","email":"%s","senha":"%s","cnpj":"45.723.174/0001-10","dataNascimento":"1980-01-01T00:00:00",
                 "razaoSocial":"Outra LTDA","nomeFantasia":"Outra","porteId":%d,"ramoId":%d,"aceitouTermos":true}"""
                .formatted(emailOutra, SENHA, porteId, ramoId))
                .andExpect(status().isCreated());
        confirmarEmail(emailOutra);
        String tokenOutra = login(emailOutra);

        int status = chamar(HttpMethod.GET, "/api/candidaturas/" + candidaturaId + "/empresa", tokenOutra, null)
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(400, 403, 404);
        int statusChat = chamar(HttpMethod.GET, "/api/mensagens/candidatura/" + candidaturaId, tokenOutra, null)
                .andReturn().getResponse().getStatus();
        assertThat(statusChat).isIn(400, 403, 404);
    }

    @Test
    @Order(12)
    @DisplayName("candidato favorita a vaga")
    void favoritos() throws Exception {
        chamar(HttpMethod.POST, "/api/favoritos/" + vagaId, tokenCandidato, null)
                .andExpect(status().is2xxSuccessful());
        chamar(HttpMethod.GET, "/api/favoritos", tokenCandidato, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem((int) vagaId)));
    }

    @Test
    @Order(13)
    @DisplayName("refresh token renova a sessão uma vez (rotação) e o logout a revoga")
    void sessao() throws Exception {
        String resposta = corpo(chamar(HttpMethod.POST, "/api/auth/refresh", null,
                "{\"refreshToken\":\"" + refreshCandidato + "\"}")
                .andExpect(status().isOk()));
        String novoRefresh = JsonPath.read(resposta, "$.refreshToken");
        assertThat(novoRefresh).isNotEqualTo(refreshCandidato);

        // O refresh antigo foi rotacionado e não serve mais.
        chamar(HttpMethod.POST, "/api/auth/refresh", null, "{\"refreshToken\":\"" + refreshCandidato + "\"}")
                .andExpect(status().is4xxClientError());

        chamar(HttpMethod.POST, "/api/auth/logout", null, "{\"refreshToken\":\"" + novoRefresh + "\"}")
                .andExpect(status().is2xxSuccessful());
        chamar(HttpMethod.POST, "/api/auth/refresh", null, "{\"refreshToken\":\"" + novoRefresh + "\"}")
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(14)
    @DisplayName("admin vê as estatísticas da plataforma")
    void estatisticas() throws Exception {
        chamar(HttpMethod.GET, "/api/admin/estatisticas", tokenAdmin, null).andExpect(status().isOk());
        chamar(HttpMethod.GET, "/api/admin/estatisticas", tokenCandidato, null).andExpect(status().isForbidden());
    }

    @Test
    @Order(15)
    @DisplayName("admin desativa o candidato e o token dele para de valer na hora")
    void desativacaoImediata() throws Exception {
        chamar(HttpMethod.GET, "/api/candidatos/meu-perfil", tokenCandidato, null).andExpect(status().isOk());

        Long usuarioId = usuariosRepository.findByEmail(emailCandidato).orElseThrow().getId();
        chamar(HttpMethod.PATCH, "/api/admin/usuarios/" + usuarioId + "/desativar", tokenAdmin, null)
                .andExpect(status().is2xxSuccessful());

        int status = chamar(HttpMethod.GET, "/api/candidatos/meu-perfil", tokenCandidato, null)
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(401, 403);
    }
}
