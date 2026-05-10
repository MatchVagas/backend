package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.SugestaoVagaResponseDTO;
import com.matchvagas.backend.dto.VagaResponseDTO;
import com.matchvagas.backend.entity.*;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.VagasMapper;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.CandidaturaRepository;
import com.matchvagas.backend.repository.VagaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Sistema de Sugestão de Vagas")
class SugestaoVagaServiceTest {

    @Mock CandidatoRepository   candidatoRepository;
    @Mock VagaRepository        vagaRepository;
    @Mock CandidaturaRepository candidaturaRepository;
    @Mock VagasMapper           vagasMapper;

    @InjectMocks SugestaoVagaService sugestaoVagaService;

    private static final Long USUARIO_ID  = 1L;
    private static final Long CANDIDATO_ID = 10L;

    private Candidatos candidato;
    private Usuarios   usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuarios();
        usuario.setId(USUARIO_ID);
        usuario.setNome("Dev Teste");
        usuario.setIdade(28);

        candidato = new Candidatos();
        candidato.setId(CANDIDATO_ID);
        candidato.setUsuario(usuario);
        candidato.setObjetivoProfissional("Desenvolvedor Java backend Spring Boot microsserviços");
        candidato.setPretensaoSalarial(new BigDecimal("7000.00"));
    }

    /** Cria uma vaga ativa com defaults razoáveis */
    private Vagas criarVaga(Long id, String titulo, String area, String requisitos,
                             BigDecimal salMin, BigDecimal salMax,
                             int idadeMin, int idadeMax) {
        StatusVaga status = new StatusVaga();
        status.setDescricao("ATIVA");

        Vagas v = new Vagas();
        v.setId(id);
        v.setTitulo(titulo);
        v.setAreaAtuacao(area);
        v.setRequisitos(requisitos);
        v.setDescricao("Descrição da vaga " + id);
        v.setSalarioMinimo(salMin);
        v.setSalarioMaximo(salMax);
        v.setIdadeMinima(idadeMin);
        v.setIdadeMaxima(idadeMax);
        v.setStatus(status);
        v.setDataExpiracao(LocalDateTime.now().plusDays(30));
        v.setDataPublicacao(LocalDateTime.now());
        v.setNumeroVagas(1);
        v.setCargaHoraria("40h");
        return v;
    }

    private VagaResponseDTO mockVagaDTO(Long id, String titulo) {
        return new VagaResponseDTO(
                id, "Tech Corp", 1L, titulo, "desc", "req",
                "beneficios", 1L, "CLT", 1L, "Remoto",
                new BigDecimal("5000"), new BigDecimal("9000"),
                "40h", 18, 45, 1L, "Superior", "TI",
                LocalDateTime.now(), LocalDateTime.now().plusDays(30),
                1L, "ATIVA", 3, 1L, "São Paulo", "SP"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Compatibilidade de Área
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Compatibilidade de área de atuação")
    class CompatibilidadeArea {

        @Test
        @DisplayName("Deve sugerir vaga quando área bate com objetivo profissional")
        void deveSugerirQuandoAreaBate() {
            Vagas vaga = criarVaga(1L, "Dev Java Senior", "Tecnologia Java Backend",
                    "Spring Boot SQL", new BigDecimal("6000"), new BigDecimal("9000"), 18, 45);

            when(candidatoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(candidato));
            when(candidaturaRepository.findByCandidatoId(CANDIDATO_ID)).thenReturn(List.of());
            when(vagaRepository.findVagasAtivas(any())).thenReturn(List.of(vaga));
            when(vagasMapper.toDTO(vaga)).thenReturn(mockVagaDTO(1L, "Dev Java Senior"));

            List<SugestaoVagaResponseDTO> result = sugestaoVagaService.sugerirVagas(USUARIO_ID);

            assertThat(result).isNotEmpty();
            assertThat(result.get(0).pontuacao()).isGreaterThan(0);
            assertThat(result.get(0).motivos()).anyMatch(m -> m.contains("Área de atuação"));
        }

        @Test
        @DisplayName("Não deve sugerir vaga sem nenhuma correspondência com o perfil")
        void naoDeveSugerirVagaSemCorrespondencia() {
            Vagas vaga = criarVaga(1L, "Contador Fiscal", "Finanças Contabilidade",
                    "Excel Contabilidade IRPF", new BigDecimal("2000"), new BigDecimal("3000"), 40, 60);

            when(candidatoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(candidato));
            when(candidaturaRepository.findByCandidatoId(CANDIDATO_ID)).thenReturn(List.of());
            when(vagaRepository.findVagasAtivas(any())).thenReturn(List.of(vaga));

            List<SugestaoVagaResponseDTO> result = sugestaoVagaService.sugerirVagas(USUARIO_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Deve sugerir vaga com keywords no título mesmo sem match na área")
        void deveSugerirPorKeywordsNoTitulo() {
            // Área diferente, mas título tem "Java"
            Vagas vaga = criarVaga(1L, "Engenheiro Java Pleno", "Sistemas de Pagamento",
                    "API REST Microsserviços", new BigDecimal("6000"), new BigDecimal("9000"), 18, 45);

            when(candidatoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(candidato));
            when(candidaturaRepository.findByCandidatoId(CANDIDATO_ID)).thenReturn(List.of());
            when(vagaRepository.findVagasAtivas(any())).thenReturn(List.of(vaga));
            when(vagasMapper.toDTO(vaga)).thenReturn(mockVagaDTO(1L, "Engenheiro Java Pleno"));

            List<SugestaoVagaResponseDTO> result = sugestaoVagaService.sugerirVagas(USUARIO_ID);

            assertThat(result).isNotEmpty();
            assertThat(result.get(0).motivos()).anyMatch(m -> m.contains("título"));
        }

        @Test
        @DisplayName("Deve sugerir vaga com keywords nos requisitos")
        void deveSugerirPorKeywordsNosRequisitos() {
            Vagas vaga = criarVaga(1L, "Analista de Sistemas", "Desenvolvimento Web",
                    "Java Spring Boot microsserviços REST", new BigDecimal("6000"), new BigDecimal("9000"), 18, 45);

            when(candidatoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(candidato));
            when(candidaturaRepository.findByCandidatoId(CANDIDATO_ID)).thenReturn(List.of());
            when(vagaRepository.findVagasAtivas(any())).thenReturn(List.of(vaga));
            when(vagasMapper.toDTO(vaga)).thenReturn(mockVagaDTO(1L, "Analista de Sistemas"));

            List<SugestaoVagaResponseDTO> result = sugestaoVagaService.sugerirVagas(USUARIO_ID);

            assertThat(result).isNotEmpty();
            assertThat(result.get(0).motivos()).anyMatch(m -> m.contains("requisitos"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Compatibilidade Salarial
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Compatibilidade salarial")
    class CompatibilidadeSalarial {

        @Test
        @DisplayName("Deve incluir motivo salarial quando pretensão está dentro da faixa")
        void deveInformarCompatibilidadeSalarial() {
            // candidato quer R$ 7.000 — faixa da vaga: 6.000–9.000
            Vagas vaga = criarVaga(1L, "Dev Java Backend", "Tecnologia Java",
                    "Spring Boot", new BigDecimal("6000"), new BigDecimal("9000"), 18, 45);

            when(candidatoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(candidato));
            when(candidaturaRepository.findByCandidatoId(CANDIDATO_ID)).thenReturn(List.of());
            when(vagaRepository.findVagasAtivas(any())).thenReturn(List.of(vaga));
            when(vagasMapper.toDTO(vaga)).thenReturn(mockVagaDTO(1L, "Dev Java Backend"));

            List<SugestaoVagaResponseDTO> result = sugestaoVagaService.sugerirVagas(USUARIO_ID);

            assertThat(result).isNotEmpty();
            assertThat(result.get(0).motivos()).anyMatch(m -> m.contains("salarial"));
            // Faixa dentro → 25 pts
            assertThat(result.get(0).pontuacao()).isGreaterThanOrEqualTo(25);
        }

        @Test
        @DisplayName("Deve dar bônus menor quando salário oferecido supera a pretensão")
        void deveInformarSalarioAcimaDaPretensao() {
            // candidato quer R$ 7.000 — vaga paga 8.000–12.000 (piso acima da pretensão)
            Vagas vaga = criarVaga(1L, "Dev Java Backend", "Tecnologia Java",
                    "Spring Boot", new BigDecimal("8000"), new BigDecimal("12000"), 18, 45);

            when(candidatoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(candidato));
            when(candidaturaRepository.findByCandidatoId(CANDIDATO_ID)).thenReturn(List.of());
            when(vagaRepository.findVagasAtivas(any())).thenReturn(List.of(vaga));
            when(vagasMapper.toDTO(vaga)).thenReturn(mockVagaDTO(1L, "Dev Java Backend"));

            List<SugestaoVagaResponseDTO> result = sugestaoVagaService.sugerirVagas(USUARIO_ID);

            assertThat(result).isNotEmpty();
            assertThat(result.get(0).motivos()).anyMatch(m -> m.contains("acima"));
        }

        @Test
        @DisplayName("Não deve dar pontos salariais quando pretensão supera o teto da vaga")
        void naoDeveContarSalarioQuandoPretensaoSuperaTeto() {
            // candidato quer R$ 7.000 — teto da vaga é R$ 5.000
            Vagas vaga = criarVaga(1L, "Dev Java Backend", "Tecnologia Java",
                    "Spring Boot", new BigDecimal("3000"), new BigDecimal("5000"), 18, 45);

            when(candidatoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(candidato));
            when(candidaturaRepository.findByCandidatoId(CANDIDATO_ID)).thenReturn(List.of());
            when(vagaRepository.findVagasAtivas(any())).thenReturn(List.of(vaga));
            when(vagasMapper.toDTO(vaga)).thenReturn(mockVagaDTO(1L, "Dev Java Backend"));

            List<SugestaoVagaResponseDTO> result = sugestaoVagaService.sugerirVagas(USUARIO_ID);

            // A vaga pode aparecer por outros critérios, mas sem motivo salarial
            if (!result.isEmpty()) {
                assertThat(result.get(0).motivos())
                        .noneMatch(m -> m.contains("salarial") || m.contains("acima"));
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Compatibilidade Etária
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Compatibilidade etária")
    class CompatibilidadeEtaria {

        @Test
        @DisplayName("Deve incluir motivo etário quando candidato está dentro da faixa")
        void deveContarPontosEtarios() {
            // candidato tem 28 anos — faixa da vaga: 25–35
            Vagas vaga = criarVaga(1L, "Dev Java Senior", "Tecnologia Java",
                    "Spring Boot", new BigDecimal("6000"), new BigDecimal("9000"), 25, 35);

            when(candidatoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(candidato));
            when(candidaturaRepository.findByCandidatoId(CANDIDATO_ID)).thenReturn(List.of());
            when(vagaRepository.findVagasAtivas(any())).thenReturn(List.of(vaga));
            when(vagasMapper.toDTO(vaga)).thenReturn(mockVagaDTO(1L, "Dev Java Senior"));

            List<SugestaoVagaResponseDTO> result = sugestaoVagaService.sugerirVagas(USUARIO_ID);

            assertThat(result).isNotEmpty();
            assertThat(result.get(0).motivos()).anyMatch(m -> m.contains("faixa etária") || m.contains("etária"));
        }

        @Test
        @DisplayName("Não deve dar pontos etários quando candidato está fora da faixa")
        void naoDeveContarPontosForaDaFaixa() {
            // candidato tem 28 anos — faixa da vaga: 40–60
            Vagas vaga = criarVaga(1L, "Dev Java Senior", "Tecnologia Java",
                    "Spring Boot", new BigDecimal("6000"), new BigDecimal("9000"), 40, 60);

            when(candidatoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(candidato));
            when(candidaturaRepository.findByCandidatoId(CANDIDATO_ID)).thenReturn(List.of());
            when(vagaRepository.findVagasAtivas(any())).thenReturn(List.of(vaga));
            when(vagasMapper.toDTO(vaga)).thenReturn(mockVagaDTO(1L, "Dev Java Senior"));

            List<SugestaoVagaResponseDTO> result = sugestaoVagaService.sugerirVagas(USUARIO_ID);

            if (!result.isEmpty()) {
                assertThat(result.get(0).motivos())
                        .noneMatch(m -> m.contains("faixa etária") || m.contains("etária"));
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ordenação e Limite
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Ordenação e limite de resultados")
    class OrdenacaoELimite {

        @Test
        @DisplayName("Vagas com maior pontuação devem aparecer primeiro")
        void deveOrdenarPorPontuacaoDecrescente() {
            // Vaga A: apenas match de título (15 pts)
            Vagas vagaA = criarVaga(1L, "Desenvolvedor Java Junior", "Sistemas ERP",
                    "Java básico", new BigDecimal("2000"), new BigDecimal("4000"), 18, 25);

            // Vaga B: match de área + título + salário + idade (30+15+25+15 = 85 pts)
            Vagas vagaB = criarVaga(2L, "Dev Java Sênior Backend", "Tecnologia Java Backend",
                    "Spring Boot microsserviços", new BigDecimal("6000"), new BigDecimal("9000"), 25, 35);

            when(candidatoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(candidato));
            when(candidaturaRepository.findByCandidatoId(CANDIDATO_ID)).thenReturn(List.of());
            when(vagaRepository.findVagasAtivas(any())).thenReturn(List.of(vagaA, vagaB));
            when(vagasMapper.toDTO(vagaA)).thenReturn(mockVagaDTO(1L, "Junior"));
            when(vagasMapper.toDTO(vagaB)).thenReturn(mockVagaDTO(2L, "Senior"));

            List<SugestaoVagaResponseDTO> result = sugestaoVagaService.sugerirVagas(USUARIO_ID);

            assertThat(result).hasSizeGreaterThan(1);
            // Vaga B deve ter pontuação maior e vir primeiro
            assertThat(result.get(0).pontuacao()).isGreaterThanOrEqualTo(result.get(1).pontuacao());
            assertThat(result.get(0).vaga().id()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Deve retornar no máximo 10 sugestões")
        void deveRetornarNoMaximo10Sugestoes() {
            List<Vagas> muitas = new java.util.ArrayList<>();
            for (long i = 1; i <= 20; i++) {
                muitas.add(criarVaga(i, "Dev Java " + i, "Tecnologia Java",
                        "Spring Boot", new BigDecimal("6000"), new BigDecimal("9000"), 20, 40));
                when(vagasMapper.toDTO(muitas.get((int)(i-1)))).thenReturn(mockVagaDTO(i, "Dev " + i));
            }

            when(candidatoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(candidato));
            when(candidaturaRepository.findByCandidatoId(CANDIDATO_ID)).thenReturn(List.of());
            when(vagaRepository.findVagasAtivas(any())).thenReturn(muitas);

            List<SugestaoVagaResponseDTO> result = sugestaoVagaService.sugerirVagas(USUARIO_ID);

            assertThat(result).hasSizeLessThanOrEqualTo(10);
        }

        @Test
        @DisplayName("Deve excluir vagas às quais o candidato já se candidatou")
        void deveExcluirVagasJaCandidatadas() {
            Vagas vaga = criarVaga(1L, "Dev Java Backend", "Tecnologia Java",
                    "Spring Boot", new BigDecimal("6000"), new BigDecimal("9000"), 18, 45);

            Candidatura candidaturaExistente = new Candidatura();
            candidaturaExistente.setVaga(vaga);

            when(candidatoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(candidato));
            when(candidaturaRepository.findByCandidatoId(CANDIDATO_ID))
                    .thenReturn(List.of(candidaturaExistente));
            when(vagaRepository.findVagasAtivas(any())).thenReturn(List.of(vaga));

            List<SugestaoVagaResponseDTO> result = sugestaoVagaService.sugerirVagas(USUARIO_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando nenhuma vaga bate com o perfil")
        void deveRetornarVazioSemMatches() {
            Vagas vaga = criarVaga(1L, "Contador Tributário", "Contabilidade Fiscal",
                    "IRPF ICMS Contabilidade", new BigDecimal("3000"), new BigDecimal("4000"), 40, 60);

            when(candidatoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(candidato));
            when(candidaturaRepository.findByCandidatoId(CANDIDATO_ID)).thenReturn(List.of());
            when(vagaRepository.findVagasAtivas(any())).thenReturn(List.of(vaga));

            List<SugestaoVagaResponseDTO> result = sugestaoVagaService.sugerirVagas(USUARIO_ID);

            assertThat(result).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Casos de Borda
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Casos de borda")
    class CasosDeBorda {

        @Test
        @DisplayName("Deve lançar exceção quando candidato não tem perfil cadastrado")
        void deveLancarExcecaoSemPerfil() {
            when(candidatoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sugestaoVagaService.sugerirVagas(USUARIO_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Perfil de candidato não encontrado");
        }

        @Test
        @DisplayName("Deve funcionar sem pretensão salarial — critério ignorado")
        void deveFuncionarSemPretensaoSalarial() {
            candidato.setPretensaoSalarial(null);
            Vagas vaga = criarVaga(1L, "Dev Java Backend", "Tecnologia Java",
                    "Spring Boot microsserviços", new BigDecimal("5000"), new BigDecimal("8000"), 18, 35);

            when(candidatoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(candidato));
            when(candidaturaRepository.findByCandidatoId(CANDIDATO_ID)).thenReturn(List.of());
            when(vagaRepository.findVagasAtivas(any())).thenReturn(List.of(vaga));
            when(vagasMapper.toDTO(vaga)).thenReturn(mockVagaDTO(1L, "Dev Java Backend"));

            List<SugestaoVagaResponseDTO> result = sugestaoVagaService.sugerirVagas(USUARIO_ID);

            // Ainda pode sugerir por área/título/requisitos
            assertThat(result).isNotEmpty();
            // Mas sem motivo salarial
            result.get(0).motivos().forEach(m ->
                    assertThat(m).doesNotContain("salarial").doesNotContain("acima"));
        }

        @Test
        @DisplayName("Deve funcionar sem objetivo profissional — apenas critérios numéricos")
        void deveFuncionarSemObjetivoProfissional() {
            candidato.setObjetivoProfissional(null);
            Vagas vaga = criarVaga(1L, "Dev Java Backend", "Tecnologia Java",
                    "Spring Boot", new BigDecimal("6000"), new BigDecimal("9000"), 18, 35);

            when(candidatoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(candidato));
            when(candidaturaRepository.findByCandidatoId(CANDIDATO_ID)).thenReturn(List.of());
            when(vagaRepository.findVagasAtivas(any())).thenReturn(List.of(vaga));
            when(vagasMapper.toDTO(vaga)).thenReturn(mockVagaDTO(1L, "Dev Java Backend"));

            // Sem objetivo, critérios de texto são zerados; salário (25 pts) e idade (15 pts) ainda valem
            List<SugestaoVagaResponseDTO> result = sugestaoVagaService.sugerirVagas(USUARIO_ID);

            assertThat(result).isNotEmpty();
            // Somente motivos numéricos presentes
            assertThat(result.get(0).motivos())
                    .noneMatch(m -> m.contains("objetivo") || m.contains("requisitos") || m.contains("título"));
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não há vagas ativas")
        void deveRetornarVazioSemVagasAtivas() {
            when(candidatoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(candidato));
            when(candidaturaRepository.findByCandidatoId(CANDIDATO_ID)).thenReturn(List.of());
            when(vagaRepository.findVagasAtivas(any())).thenReturn(List.of());

            assertThat(sugestaoVagaService.sugerirVagas(USUARIO_ID)).isEmpty();
        }
    }
}
