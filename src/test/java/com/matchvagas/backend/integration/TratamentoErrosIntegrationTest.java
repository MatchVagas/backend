package com.matchvagas.backend.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Erros de requisição viram 4xx com corpo padronizado — nunca 500. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Integração — tratamento de erros HTTP")
class TratamentoErrosIntegrationTest {

    @Autowired MockMvc mockMvc;

    private RequestPostProcessor como(String perfil) {
        return user("999999").authorities(new SimpleGrantedAuthority(perfil));
    }

    @Test
    @DisplayName("validação de campos devolve 400 com o mapa de erros por campo")
    void validacao() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Erro de validação"))
                .andExpect(jsonPath("$.erros").isMap());
    }

    @Test
    @DisplayName("recurso inexistente devolve 404")
    void naoEncontrado() throws Exception {
        mockMvc.perform(get("/api/vagas/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("JSON malformado devolve 400")
    void jsonMalformado() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{ quebrado"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("id não numérico na URL devolve 400")
    void tipoInvalidoNaUrl() throws Exception {
        mockMvc.perform(get("/api/vagas/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("parâmetro obrigatório ausente devolve 400")
    void parametroAusente() throws Exception {
        mockMvc.perform(patch("/api/alertas/1").with(como("CANDIDATO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Content-Type não suportado devolve 415")
    void tipoDeMidiaNaoSuportado() throws Exception {
        mockMvc.perform(post("/api/candidatos/curriculo/upload").with(como("CANDIDATO"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("arquivo multipart ausente devolve 400 dizendo o campo")
    void parteMultipartAusente() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/candidatos/curriculo/upload").with(como("CANDIDATO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("arquivo")));
    }

    @Test
    @DisplayName("método HTTP não suportado devolve 405")
    void metodoNaoSuportado() throws Exception {
        mockMvc.perform(put("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("rota inexistente devolve 404")
    void rotaInexistente() throws Exception {
        mockMvc.perform(get("/api/nao-existe").with(como("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("violação de restrição do banco devolve 4xx sem vazar SQL")
    void violacaoDeIntegridade() throws Exception {
        mockMvc.perform(post("/api/lookup/vagas/tipos").with(como("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message").value(not(containsString("SQL"))));
    }

    @Test
    @DisplayName("sem autenticação devolve 401 ou 403; token forjado também")
    void semAutenticacao() throws Exception {
        mockMvc.perform(get("/api/candidatos/meu-perfil"))
                .andExpect(result -> org.assertj.core.api.Assertions
                        .assertThat(result.getResponse().getStatus()).isIn(401, 403));
        mockMvc.perform(get("/api/candidatos/meu-perfil")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.assinatura-falsa"))
                .andExpect(result -> org.assertj.core.api.Assertions
                        .assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    @DisplayName("respostas trazem os headers de segurança")
    void headersDeSeguranca() throws Exception {
        mockMvc.perform(get("/api/vagas"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }
}
