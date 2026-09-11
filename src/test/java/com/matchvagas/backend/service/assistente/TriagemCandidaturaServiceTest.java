package com.matchvagas.backend.service.assistente;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchvagas.backend.dto.TriagemResponseDTO;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Candidatura;
import com.matchvagas.backend.entity.Curriculos;
import com.matchvagas.backend.entity.Empresas;
import com.matchvagas.backend.entity.Experiencia;
import com.matchvagas.backend.entity.Habilidade;
import com.matchvagas.backend.entity.RecomendacaoTriagem;
import com.matchvagas.backend.entity.TriagemCandidatura;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.entity.Vagas;
import com.matchvagas.backend.repository.CandidaturaRepository;
import com.matchvagas.backend.repository.EmpresaRepository;
import com.matchvagas.backend.repository.ExperienciaRepository;
import com.matchvagas.backend.repository.FormacaoRepository;
import com.matchvagas.backend.repository.TriagemCandidaturaRepository;
import com.matchvagas.backend.service.AuditoriaService;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.matchvagas.backend.service.assistente.PromptsAssistivos.NAO_COMPARTILHADO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fase 3 — triagem inicial assistida")
class TriagemCandidaturaServiceTest {
    private static final String RESPOSTA = """
            {"parecer": "Atende ao requisito de Java.", "pontosFortes": ["Java"],
             "lacunas": [], "recomendacao": "AVANCAR"}
            """;

    @Mock CandidaturaRepository candidaturaRepository;
    @Mock EmpresaRepository empresaRepository;
    @Mock ExperienciaRepository experienciaRepository;
    @Mock FormacaoRepository formacaoRepository;
    @Mock TriagemCandidaturaRepository triagemRepository;
    @Mock AuditoriaService auditoriaService;
    @Mock LlmPort port;

    private TriagemCandidaturaService service;
    private Empresas empresa;
    private Candidatura candidatura;

    @BeforeEach
    void setUp() {
        LlmEstruturado llm = new LlmEstruturado(port, new ObjectMapper());
        service = new TriagemCandidaturaService(candidaturaRepository, empresaRepository, experienciaRepository,
                formacaoRepository, triagemRepository, llm, new CotaAssistente(llm, new RateLimiterService(), 30),
                auditoriaService, new TransactionTemplate(mock(PlatformTransactionManager.class)));
        ReflectionTestUtils.setField(service, "maxCaracteresEntrada", 12_000);

        empresa = new Empresas();
        empresa.setId(10L);
        Vagas vaga = new Vagas();
        vaga.setId(20L);
        vaga.setEmpresas(empresa);
        vaga.setTitulo("Desenvolvedor Java");
        vaga.setAreaAtuacao("Tecnologia");
        vaga.setDescricao("Time de backend.");
        vaga.setRequisitos("Java e SQL");

        Usuarios usuario = new Usuarios();
        usuario.setNome("Maria Secreta");
        usuario.setEmail("maria@secreta.com");
        Curriculos curriculo = new Curriculos();
        curriculo.setTextoExtraido("Texto do currículo com Java avançado.");
        Candidatos candidato = new Candidatos();
        candidato.setId(30L);
        candidato.setUsuario(usuario);
        candidato.setObjetivoProfissional("Desenvolvedora backend");
        candidato.setPretensaoSalarial(new BigDecimal("9000"));
        candidato.setHabilidades(List.of(new Habilidade("Kotlin", null)));
        candidato.setCurriculo(curriculo);

        candidatura = new Candidatura();
        candidatura.setId(50L);
        candidatura.setVaga(vaga);
        candidatura.setCandidato(candidato);
        when(candidaturaRepository.findById(50L)).thenReturn(Optional.of(candidatura));
    }

    @Test
    void respeitaOCompartilhamentoENuncaEnviaIdentificacao() {
        candidatura.setCompartilharCurriculo(false);
        candidatura.setCompartilharPretensaoSalarial(false);
        prepararGeracao(new AtomicReference<>());
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        when(port.gerarJson(eq(PromptsAssistivos.SISTEMA_TRIAGEM), prompt.capture())).thenReturn(RESPOSTA);

        TriagemResponseDTO triagem = service.gerar(50L, 5L, false);

        assertThat(prompt.getValue())
                .contains("Desenvolvedor Java", "Desenvolvedora backend", "Engenheira de Software",
                        "Currículo: " + NAO_COMPARTILHADO, "Pretensão salarial: " + NAO_COMPARTILHADO)
                .doesNotContain("Maria Secreta", "maria@secreta.com", "Kotlin", "Java avançado", "9.000");
        assertThat(triagem.recomendacao()).isEqualTo(RecomendacaoTriagem.AVANCAR);
        assertThat(triagem.pontosFortes()).containsExactly("Java");
        assertThat(triagem.lacunas()).isEmpty();
        assertThat(triagem.aviso()).isEqualTo(TriagemResponseDTO.AVISO);
        verify(auditoriaService).registrar(5L, 30L, "TRIAGEM_IA", "CREATE");
    }

    @Test
    void reaproveitaATriagemAteAEntradaMudar() {
        AtomicReference<TriagemCandidatura> salva = new AtomicReference<>();
        prepararGeracao(salva);
        when(port.gerarJson(anyString(), anyString())).thenReturn(RESPOSTA);

        service.gerar(50L, 5L, false);
        service.gerar(50L, 5L, false);
        verify(port, times(1)).gerarJson(anyString(), anyString());

        // O candidato revogou o compartilhamento do currículo: a triagem salva ficou desatualizada.
        candidatura.setCompartilharCurriculo(false);
        assertThat(service.buscar(50L, 5L).desatualizado()).isTrue();
    }

    @Test
    void empresaDeOutraVagaNaoTemAcesso() {
        Empresas outra = new Empresas();
        outra.setId(99L);
        when(empresaRepository.findByUsuarioId(6L)).thenReturn(Optional.of(outra));

        assertThatThrownBy(() -> service.gerar(50L, 6L, false)).isInstanceOf(AccessDeniedException.class);
        verify(port, never()).gerarJson(anyString(), anyString());
    }

    private void prepararGeracao(AtomicReference<TriagemCandidatura> salva) {
        Experiencia experiencia = new Experiencia();
        experiencia.setCargo("Engenheira de Software");
        experiencia.setEmpresa("ACME");
        when(empresaRepository.findByUsuarioId(5L)).thenReturn(Optional.of(empresa));
        when(experienciaRepository.findByCandidatoId(30L)).thenReturn(List.of(experiencia));
        when(formacaoRepository.findByCandidatoId(30L)).thenReturn(List.of());
        when(triagemRepository.findByCandidaturaId(50L)).thenAnswer(inv -> Optional.ofNullable(salva.get()));
        when(triagemRepository.save(any(TriagemCandidatura.class))).thenAnswer(inv -> {
            salva.set(inv.getArgument(0));
            return inv.getArgument(0);
        });
        when(candidaturaRepository.getReferenceById(50L)).thenReturn(candidatura);
        when(port.isAtivo()).thenReturn(true);
        when(port.modelo()).thenReturn("qwen2.5:7b");
    }
}
