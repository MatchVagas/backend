package com.matchvagas.backend.service;

import com.matchvagas.backend.exception.BusinessException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Integração com o Supabase Storage (servidor HTTP local)")
class SupabaseStorageServiceTest {

    private record Chamada(String metodo, String caminho, String autorizacao, String upsert, String corpo) {}

    private HttpServer servidor;
    private final List<Chamada> chamadas = new ArrayList<>();
    private int status = 200;
    private String resposta = "{}";
    private SupabaseStorageService service;

    @BeforeEach
    void setUp() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", troca -> {
            String corpo = new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            chamadas.add(new Chamada(troca.getRequestMethod(), troca.getRequestURI().getPath(),
                    troca.getRequestHeaders().getFirst("Authorization"),
                    troca.getRequestHeaders().getFirst("x-upsert"), corpo));
            byte[] bytes = resposta.getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().add("Content-Type", "application/json");
            troca.sendResponseHeaders(status, bytes.length);
            troca.getResponseBody().write(bytes);
            troca.close();
        });
        servidor.start();

        service = new SupabaseStorageService();
        ReflectionTestUtils.setField(service, "supabaseUrl", "http://127.0.0.1:" + servidor.getAddress().getPort());
        ReflectionTestUtils.setField(service, "serviceRoleKey", "chave-servico");
        ReflectionTestUtils.setField(service, "bucket", "curriculos");
        ReflectionTestUtils.setField(service, "imagesBucket", "imagens-perfil");
        ReflectionTestUtils.invokeMethod(service, "init");
    }

    @AfterEach
    void tearDown() {
        servidor.stop(0);
    }

    @Test
    @DisplayName("upload faz PUT com upsert e a chave de serviço")
    void upload() {
        service.upload("10/cv.pdf", "%PDF".getBytes(), "application/pdf");

        Chamada c = chamadas.getFirst();
        assertThat(c.metodo()).isEqualTo("PUT");
        assertThat(c.caminho()).isEqualTo("/storage/v1/object/curriculos/10/cv.pdf");
        assertThat(c.autorizacao()).isEqualTo("Bearer chave-servico");
        assertThat(c.upsert()).isEqualTo("true");
        assertThat(c.corpo()).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("URL assinada recebe o prefixo /storage/v1 e respeita a validade pedida")
    void urlAssinada() {
        resposta = "{\"signedURL\":\"/object/sign/curriculos/10/cv.pdf?token=abc\"}";

        String url = service.gerarUrlAssinada("10/cv.pdf", 3600);

        assertThat(url).endsWith("/storage/v1/object/sign/curriculos/10/cv.pdf?token=abc");
        assertThat(chamadas.getFirst().caminho()).isEqualTo("/storage/v1/object/sign/curriculos/10/cv.pdf");
        assertThat(chamadas.getFirst().corpo()).contains("\"expiresIn\":3600");
    }

    @Test
    @DisplayName("resposta sem signedURL é erro de negócio")
    void urlAssinadaInvalida() {
        resposta = "{\"error\":\"not found\"}";

        assertThatThrownBy(() -> service.gerarUrlAssinadaImagem("a.png", 60))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("remoção usa DELETE no bucket com a lista de prefixos")
    void deletar() {
        service.deletarImagem("candidatos/10/f.png");

        Chamada c = chamadas.getFirst();
        assertThat(c.metodo()).isEqualTo("DELETE");
        assertThat(c.caminho()).isEqualTo("/storage/v1/object/imagens-perfil");
        assertThat(c.corpo()).contains("candidatos/10/f.png");
    }

    @Test
    @DisplayName("erro HTTP do storage vira BusinessException")
    void erroHttp() {
        status = 500;

        assertThatThrownBy(() -> service.upload("x.pdf", new byte[]{1}, "application/pdf"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("storage");
        assertThatThrownBy(() -> service.deletar("x.pdf")).isInstanceOf(BusinessException.class);
    }
}
