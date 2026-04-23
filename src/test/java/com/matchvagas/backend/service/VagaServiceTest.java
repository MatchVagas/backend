package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.VagaRequestDTO;
import com.matchvagas.backend.dto.VagaResponseDTO;
import com.matchvagas.backend.entity.*;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.VagasMapper;
import com.matchvagas.backend.repository.*;
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
@DisplayName("RF005, RF006 e RF007 — Vagas")
class VagaServiceTest {

    @Mock VagaRepository vagaRepository;
    @Mock EmpresaRepository empresaRepository;
    @Mock TipoVagaRepository tipoVagaRepository;
    @Mock ModalidadeRepository modalidadeRepository;
    @Mock EscolaridadeRepository escolaridadeRepository;
    @Mock StatusVagaRepository statusVagaRepository;
    @Mock CidadeRepository cidadeRepository;
    @Mock VagasMapper vagasMapper;

    @InjectMocks VagaService vagaService;

    private Vagas vaga;
    private VagaRequestDTO request;
    private VagaResponseDTO responseDTO;
    private Empresas empresa;
    private TipoVaga tipoVaga;
    private Modalidade modalidade;
    private Escolaridades escolaridade;
    private StatusVaga statusVaga;
    private Cidade cidade;

    @BeforeEach
    void setUp() {
        empresa = new Empresas();
        empresa.setId(1L);
        empresa.setNomeFantasia("Tech Corp");

        tipoVaga = new TipoVaga();
        tipoVaga.setId(1L);
        tipoVaga.setDescricao("CLT");

        modalidade = new Modalidade();
        modalidade.setId(1L);
        modalidade.setDescricao("Remoto");

        escolaridade = new Escolaridades();
        escolaridade.setId(1L);
        escolaridade.setNome("Ensino Superior");

        statusVaga = new StatusVaga();
        statusVaga.setId(1L);
        statusVaga.setDescricao("ATIVA");

        cidade = new Cidade();
        cidade.setId(1L);
        cidade.setNome("São Paulo");

        vaga = new Vagas();
        vaga.setId(1L);
        vaga.setTitulo("Dev Java Pleno");
        vaga.setDescricao("Vaga para desenvolvedor Java com experiência em Spring Boot");
        vaga.setRequisitos("Java 17, Spring Boot, JPA");
        vaga.setEmpresas(empresa);
        vaga.setTipoVaga(tipoVaga);
        vaga.setModalidade(modalidade);
        vaga.setEscolaridade(escolaridade);
        vaga.setStatus(statusVaga);
        vaga.setCidade(cidade);
        vaga.setSalarioMinimo(new BigDecimal("5000.00"));
        vaga.setSalarioMaximo(new BigDecimal("8000.00"));
        vaga.setIdadeMinima(18);
        vaga.setIdadeMaxima(40);
        vaga.setNumeroVagas(3);
        vaga.setCargaHoraria("40h/semana");
        vaga.setAreaAtuacao("Tecnologia");
        vaga.setDataExpiracao(LocalDateTime.now().plusDays(30));

        request = new VagaRequestDTO(
                1L, "Dev Java Pleno",
                "Vaga para desenvolvedor Java com experiência em Spring Boot",
                "Java 17, Spring Boot, JPA",
                1L, 1L,
                new BigDecimal("5000.00"), new BigDecimal("8000.00"),
                "Vale refeição, Plano de saúde",
                "40h/semana", 18, 40, 1L,
                "Tecnologia",
                LocalDateTime.now().plusDays(30),
                1L, 3, 1L
        );

        responseDTO = new VagaResponseDTO(
                "Tech Corp", "Dev Java Pleno",
                "Vaga para desenvolvedor Java com experiência em Spring Boot",
                "Java 17, Spring Boot, JPA",
                "Vale refeição, Plano de saúde",
                1L, "CLT", 1L, "Remoto",
                new BigDecimal("5000.00"), new BigDecimal("8000.00"),
                "40h/semana", 18, 40,
                1L, "Ensino Superior",
                "Tecnologia",
                LocalDateTime.now(), LocalDateTime.now().plusDays(30),
                1L, "ATIVA", 3,
                1L, "São Paulo", "SP"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RF005 — Cadastro de Vagas
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RF005 — Cadastro de Vagas")
    class RF005 {

        @Test
        @DisplayName("Deve cadastrar vaga com sucesso")
        void deveCadastrarVagaComSucesso() {
            when(vagasMapper.toEntity(request)).thenReturn(vaga);
            when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
            when(tipoVagaRepository.findById(1L)).thenReturn(Optional.of(tipoVaga));
            when(modalidadeRepository.findById(1L)).thenReturn(Optional.of(modalidade));
            when(escolaridadeRepository.findById(1L)).thenReturn(Optional.of(escolaridade));
            when(statusVagaRepository.findById(1L)).thenReturn(Optional.of(statusVaga));
            when(cidadeRepository.findById(1L)).thenReturn(Optional.of(cidade));
            when(vagaRepository.save(any())).thenReturn(vaga);
            when(vagasMapper.toDTO(vaga)).thenReturn(responseDTO);

            VagaResponseDTO result = vagaService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.titulo()).isEqualTo("Dev Java Pleno");
            assertThat(result.nomeFantasiaEmpresa()).isEqualTo("Tech Corp");
            verify(vagaRepository).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando salário mínimo maior que máximo")
        void deveLancarExcecaoSalarioInvalido() {
            VagaRequestDTO requestInvalido = new VagaRequestDTO(
                    1L, "Dev Java", "Descrição longa o suficiente para validar", "Requisitos",
                    1L, 1L,
                    new BigDecimal("9000.00"), new BigDecimal("5000.00"), // mínimo > máximo
                    null, "40h/semana", 18, 40, 1L, "TI",
                    LocalDateTime.now().plusDays(30), 1L, 1, 1L
            );

            when(vagasMapper.toEntity(any())).thenReturn(vaga);
            when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
            when(tipoVagaRepository.findById(1L)).thenReturn(Optional.of(tipoVaga));
            when(modalidadeRepository.findById(1L)).thenReturn(Optional.of(modalidade));
            when(escolaridadeRepository.findById(1L)).thenReturn(Optional.of(escolaridade));
            when(statusVagaRepository.findById(1L)).thenReturn(Optional.of(statusVaga));
            when(cidadeRepository.findById(1L)).thenReturn(Optional.of(cidade));

            assertThatThrownBy(() -> vagaService.create(requestInvalido))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Salário mínimo");
        }

        @Test
        @DisplayName("Deve lançar exceção quando empresa não encontrada")
        void deveLancarExcecaoEmpresaNaoEncontrada() {
            when(vagasMapper.toEntity(any())).thenReturn(vaga);
            when(empresaRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vagaService.create(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Empresa");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RF006 — Manutenção de Vagas
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RF006 — Manutenção de Vagas")
    class RF006 {

        @Test
        @DisplayName("Deve atualizar vaga existente com sucesso")
        void deveAtualizarVagaComSucesso() {
            VagaResponseDTO responseAtualizado = new VagaResponseDTO(
                    "Tech Corp", "Dev Java Sênior",
                    "Vaga atualizada", "Java 21",
                    null, 1L, "CLT", 1L, "Remoto",
                    new BigDecimal("8000.00"), new BigDecimal("12000.00"),
                    "40h/semana", 25, 45,
                    1L, "Ensino Superior", "Tecnologia",
                    LocalDateTime.now(), LocalDateTime.now().plusDays(30),
                    1L, "ATIVA", 2,
                    1L, "São Paulo", "SP"
            );

            when(vagaRepository.findById(1L)).thenReturn(Optional.of(vaga));
            when(tipoVagaRepository.findById(1L)).thenReturn(Optional.of(tipoVaga));
            when(modalidadeRepository.findById(1L)).thenReturn(Optional.of(modalidade));
            when(escolaridadeRepository.findById(1L)).thenReturn(Optional.of(escolaridade));
            when(statusVagaRepository.findById(1L)).thenReturn(Optional.of(statusVaga));
            when(cidadeRepository.findById(1L)).thenReturn(Optional.of(cidade));
            when(vagaRepository.save(any())).thenReturn(vaga);
            when(vagasMapper.toDTO(any())).thenReturn(responseAtualizado);

            VagaResponseDTO result = vagaService.update(1L, request);

            assertThat(result).isNotNull();
            verify(vagaRepository).save(any());
        }

        @Test
        @DisplayName("Deve remover vaga existente")
        void deveRemoverVagaComSucesso() {
            when(vagaRepository.existsById(1L)).thenReturn(true);
            doNothing().when(vagaRepository).deleteById(1L);

            vagaService.delete(1L);

            verify(vagaRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Deve lançar exceção ao remover vaga inexistente")
        void deveLancarExcecaoRemoverVagaInexistente() {
            when(vagaRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> vagaService.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(vagaRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar vaga inexistente")
        void deveLancarExcecaoAtualizarVagaInexistente() {
            when(vagaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vagaService.update(99L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RF007 — Busca e Filtragem de Vagas
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RF007 — Busca e Filtragem de Vagas")
    class RF007 {

        @Test
        @DisplayName("Deve retornar todas as vagas quando sem filtros")
        void deveRetornarTodasVagasSemFiltro() {
            when(vagaRepository.findAll()).thenReturn(List.of(vaga));
            when(vagasMapper.toDTO(vaga)).thenReturn(responseDTO);

            List<VagaResponseDTO> result = vagaService.search(null, null, null, null);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Deve filtrar vagas por título")
        void deveFilterPorTitulo() {
            when(vagaRepository.findByTituloContaining("Java")).thenReturn(List.of(vaga));
            when(vagasMapper.toDTO(vaga)).thenReturn(responseDTO);

            List<VagaResponseDTO> result = vagaService.search("Java", null, null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).titulo()).isEqualTo("Dev Java Pleno");
        }

        @Test
        @DisplayName("Deve filtrar vagas por área de atuação")
        void deveFiltrarPorAreaAtuacao() {
            when(vagaRepository.findByDescricaoContaining("Tecnologia")).thenReturn(List.of(vaga));
            when(vagasMapper.toDTO(vaga)).thenReturn(responseDTO);

            List<VagaResponseDTO> result = vagaService.search(null, "Tecnologia", null, null);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando nenhuma vaga bate com o filtro")
        void deveRetornarVazioQuandoSemCorrespondencia() {
            when(vagaRepository.findByTituloContaining("Python")).thenReturn(List.of());

            List<VagaResponseDTO> result = vagaService.search("Python", null, null, null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Deve buscar vaga por ID")
        void deveBuscarPorId() {
            when(vagaRepository.findById(1L)).thenReturn(Optional.of(vaga));
            when(vagasMapper.toDTO(vaga)).thenReturn(responseDTO);

            VagaResponseDTO result = vagaService.findById(1L);

            assertThat(result.titulo()).isEqualTo("Dev Java Pleno");
        }

        @Test
        @DisplayName("Deve lançar exceção para vaga não encontrada")
        void deveLancarExcecaoVagaNaoEncontrada() {
            when(vagaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vagaService.findById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
