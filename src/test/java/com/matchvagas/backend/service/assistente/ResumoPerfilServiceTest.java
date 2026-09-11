package com.matchvagas.backend.service.assistente;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Curriculos;
import com.matchvagas.backend.entity.Habilidade;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.LlmIndisponivelException;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.CurriculoRepository;
import com.matchvagas.backend.service.RateLimiterService;
import com.matchvagas.backend.service.llm.LlmEstruturado;
import com.matchvagas.backend.service.llm.LlmPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fase 3 — resumo de perfil a partir do currículo")
class ResumoPerfilServiceTest {
    private static final String RESPOSTA = """
            {"objetivoProfissional": "Sou desenvolvedora backend com foco em Java.",
             "habilidades": ["java", "Spring Boot, SQL", "Comunicação", "comunicacao"]}
            """;

    @Mock CandidatoRepository candidatoRepository;
    @Mock CurriculoRepository curriculoRepository;
    @Mock LlmPort port;

    private ResumoPerfilService service;
    private Candidatos candidato;
    private Curriculos curriculo;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        LlmEstruturado llm = new LlmEstruturado(port, mapper);
        service = new ResumoPerfilService(candidatoRepository, curriculoRepository, llm,
                new CotaAssistente(llm, new RateLimiterService(), 30), mapper,
                new TransactionTemplate(mock(PlatformTransactionManager.class)));
        ReflectionTestUtils.setField(service, "maxCaracteresEntrada", 12_000);

        candidato = new Candidatos();
        candidato.setId(30L);
        candidato.setHabilidades(new ArrayList<>(List.of(new Habilidade("Java", null))));
        curriculo = new Curriculos();
        curriculo.setId(40L);
        curriculo.setTextoExtraido("Desenvolvedora Java há 5 anos. Contato: maria@email.com");
        when(candidatoRepository.findByUsuarioId(5L)).thenReturn(Optional.of(candidato));
        when(curriculoRepository.findByCandidatoId(30L)).thenReturn(Optional.of(curriculo));
    }

    @Test
    void sugereSemDuplicarHabilidadesDoPerfilNemEnviarContato() {
        ativarLlm();
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        when(port.gerarJson(eq(PromptsAssistivos.SISTEMA_RESUMO), prompt.capture())).thenReturn(RESPOSTA);

        var resumo = service.gerar(5L, false);

        assertThat(resumo.objetivoProfissional()).isEqualTo("Sou desenvolvedora backend com foco em Java.");
        assertThat(resumo.habilidadesSugeridas()).containsExactly("Spring Boot", "SQL", "Comunicação");
        assertThat(resumo.desatualizado()).isFalse();
        assertThat(prompt.getValue()).doesNotContain("maria@email.com").contains("[e-mail omitido]");
        assertThat(curriculo.getResumoIa()).isNotBlank();
        assertThat(curriculo.getResumoIaModelo()).isEqualTo("qwen2.5:7b");
        verify(curriculoRepository).save(curriculo);
    }

    @Test
    void reaproveitaASugestaoEnquantoOCurriculoNaoMuda() {
        ativarLlm();
        when(port.gerarJson(anyString(), anyString())).thenReturn(RESPOSTA);

        service.gerar(5L, false);
        var segunda = service.gerar(5L, false);

        assertThat(segunda.habilidadesSugeridas()).containsExactly("Spring Boot", "SQL", "Comunicação");
        verify(port, times(1)).gerarJson(anyString(), anyString());
    }

    @Test
    void indicaSugestaoDesatualizadaQuandoOCurriculoMuda() {
        ativarLlm();
        when(port.gerarJson(anyString(), anyString())).thenReturn(RESPOSTA);
        service.gerar(5L, false);

        curriculo.setTextoExtraido("Novo currículo: gerente de projetos.");

        assertThat(service.buscar(5L).desatualizado()).isTrue();
    }

    @Test
    void llmDesligadoResponde503SemChamarOModelo() {
        when(port.isAtivo()).thenReturn(false);

        assertThatThrownBy(() -> service.gerar(5L, false)).isInstanceOf(LlmIndisponivelException.class);
        verify(port, never()).gerarJson(anyString(), anyString());
    }

    @Test
    void curriculoSemTextoExtraidoPedeReenvio() {
        curriculo.setTextoExtraido(null);

        assertThatThrownBy(() -> service.gerar(5L, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Envie o arquivo novamente");
    }

    private void ativarLlm() {
        when(port.isAtivo()).thenReturn(true);
        when(port.modelo()).thenReturn("qwen2.5:7b");
        when(curriculoRepository.findById(40L)).thenReturn(Optional.of(curriculo));
    }
}
