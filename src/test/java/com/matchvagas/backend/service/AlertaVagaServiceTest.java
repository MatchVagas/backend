package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.AlertaVagaRequestDTO;
import com.matchvagas.backend.dto.AlertaVagaResponseDTO;
import com.matchvagas.backend.entity.AlertaVaga;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Cidade;
import com.matchvagas.backend.entity.Modalidade;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.entity.Vagas;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.repository.AlertaVagaRepository;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.CidadeRepository;
import com.matchvagas.backend.repository.ModalidadeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fase 2 — Alertas de vaga")
class AlertaVagaServiceTest {

    @Mock AlertaVagaRepository alertaRepository;
    @Mock CandidatoRepository  candidatoRepository;
    @Mock CidadeRepository     cidadeRepository;
    @Mock ModalidadeRepository modalidadeRepository;
    @Mock NotificacaoService   notificacaoService;

    @InjectMocks AlertaVagaService service;

    private static final Long USUARIO_ID   = 1L;
    private static final Long CANDIDATO_ID = 10L;

    private Candidatos candidato;

    @BeforeEach
    void setUp() {
        Usuarios usuario = new Usuarios();
        usuario.setId(USUARIO_ID);
        candidato = new Candidatos();
        candidato.setId(CANDIDATO_ID);
        candidato.setUsuario(usuario);
    }

    private Vagas vaga(String area, Long cidadeId, Long modalidadeId, String salarioMax) {
        Vagas v = new Vagas();
        v.setId(5L);
        v.setTitulo("Dev Java");
        v.setAreaAtuacao(area);
        if (cidadeId != null) {
            Cidade c = new Cidade();
            c.setId(cidadeId);
            v.setCidade(c);
        }
        if (modalidadeId != null) {
            Modalidade m = new Modalidade();
            m.setId(modalidadeId);
            v.setModalidade(m);
        }
        if (salarioMax != null) v.setSalarioMaximo(new BigDecimal(salarioMax));
        return v;
    }

    private AlertaVaga alerta(String area, Long cidadeId, Long modalidadeId, String salarioMin) {
        AlertaVaga a = new AlertaVaga();
        a.setCandidato(candidato);
        a.setAreaAtuacao(area);
        a.setCidadeId(cidadeId);
        a.setModalidadeId(modalidadeId);
        if (salarioMin != null) a.setSalarioMinimoDesejado(new BigDecimal(salarioMin));
        a.setAtivo(true);
        return a;
    }

    @Nested
    @DisplayName("Criar")
    class Criar {

        @Test
        @DisplayName("Cria alerta com ativo=true por padrão quando não informado")
        void criaComDefaults() {
            AlertaVagaRequestDTO dto = new AlertaVagaRequestDTO("Java", null, null, null, null);
            when(candidatoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(candidato));
            when(alertaRepository.save(any(AlertaVaga.class))).thenAnswer(inv -> {
                AlertaVaga a = inv.getArgument(0);
                a.setId(77L);
                return a;
            });

            AlertaVagaResponseDTO res = service.criar(USUARIO_ID, dto);

            assertThat(res.id()).isEqualTo(77L);
            assertThat(res.ativo()).isTrue();
            assertThat(res.areaAtuacao()).isEqualTo("Java");
        }

        @Test
        @DisplayName("Cidade inexistente → ResourceNotFoundException")
        void cidadeInexistente() {
            AlertaVagaRequestDTO dto = new AlertaVagaRequestDTO(null, 999L, null, null, null);
            when(candidatoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(candidato));
            when(cidadeRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> service.criar(USUARIO_ID, dto))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(alertaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Matching (casa)")
    class Matching {

        @Test
        @DisplayName("Casa quando todos os critérios batem")
        void casaTudo() {
            assertThat(service.casa(
                    alerta("java", 3L, 1L, "5000"),
                    vaga("Desenvolvedor Java", 3L, 1L, "8000"))).isTrue();
        }

        @Test
        @DisplayName("Não casa quando a cidade difere")
        void naoCasaCidade() {
            assertThat(service.casa(
                    alerta(null, 3L, null, null),
                    vaga("Java", 9L, null, null))).isFalse();
        }

        @Test
        @DisplayName("Não casa quando o teto salarial da vaga é menor que o desejado")
        void naoCasaSalario() {
            assertThat(service.casa(
                    alerta(null, null, null, "9000"),
                    vaga("Java", null, null, "8000"))).isFalse();
        }

        @Test
        @DisplayName("Alerta sem critérios casa com qualquer vaga")
        void semCriteriosCasaTudo() {
            assertThat(service.casa(
                    alerta(null, null, null, null),
                    vaga("Qualquer", 1L, 1L, "1000"))).isTrue();
        }
    }

    @Nested
    @DisplayName("Notificar nova vaga")
    class Notificar {

        @Test
        @DisplayName("Notifica só os candidatos com alerta compatível")
        void notificaCompativeis() {
            AlertaVaga compat = alerta("java", null, null, null);

            Usuarios outro = new Usuarios();
            outro.setId(2L);
            Candidatos outroCand = new Candidatos();
            outroCand.setId(20L);
            outroCand.setUsuario(outro);
            AlertaVaga incompat = new AlertaVaga();
            incompat.setCandidato(outroCand);
            incompat.setAreaAtuacao("python");
            incompat.setAtivo(true);

            when(alertaRepository.findByAtivoTrue()).thenReturn(List.of(compat, incompat));

            service.notificarNovaVaga(vaga("Desenvolvedor Java", null, null, null));

            verify(notificacaoService).notificarPorTipo(
                    eq(USUARIO_ID), eq("Nova vaga para você"), any(), eq("Alerta"));
            verify(notificacaoService, never()).notificarPorTipo(eq(2L), any(), any(), any());
        }

        @Test
        @DisplayName("Mesmo candidato com dois alertas compatíveis é notificado uma vez")
        void dedupPorCandidato() {
            when(alertaRepository.findByAtivoTrue()).thenReturn(List.of(
                    alerta("java", null, null, null),
                    alerta(null, null, null, "1000")));

            service.notificarNovaVaga(vaga("Java", null, null, "5000"));

            verify(notificacaoService, times(1)).notificarPorTipo(
                    eq(USUARIO_ID), any(), any(), eq("Alerta"));
        }
    }
}
