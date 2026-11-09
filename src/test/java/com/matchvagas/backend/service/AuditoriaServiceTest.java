package com.matchvagas.backend.service;

import com.matchvagas.backend.entity.AuditoriaAcesso;
import com.matchvagas.backend.repository.AuditoriaAcessoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LGPD art. 37 — trilha de auditoria")
class AuditoriaServiceTest {

    @Mock AuditoriaAcessoRepository repository;
    @InjectMocks AuditoriaService service;

    @AfterEach
    void limpar() {
        RequestContextHolder.resetRequestAttributes();
    }

    private AuditoriaAcesso registrar() {
        service.registrar(5L, 30L, "CURRICULO", "READ");
        ArgumentCaptor<AuditoriaAcesso> captor = ArgumentCaptor.forClass(AuditoriaAcesso.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("grava quem acessou qual recurso, e o IP da requisição")
    void gravaComIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.9");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        AuditoriaAcesso registro = registrar();

        assertThat(registro.getUsuarioSolicitanteId()).isEqualTo(5L);
        assertThat(registro.getRecursoId()).isEqualTo(30L);
        assertThat(registro.getTipoRecurso()).isEqualTo("CURRICULO");
        assertThat(registro.getOperacao()).isEqualTo("READ");
        assertThat(registro.getIpOrigem()).isEqualTo("10.0.0.9");
    }

    @Test
    @DisplayName("usa o primeiro IP do X-Forwarded-For quando presente")
    void usaXff() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "200.1.1.1, 10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(registrar().getIpOrigem()).isEqualTo("200.1.1.1");
    }

    @Test
    @DisplayName("fora de uma requisição (ex.: job agendado) grava sem IP")
    void semRequisicao() {
        assertThat(registrar().getIpOrigem()).isNull();
    }
}
