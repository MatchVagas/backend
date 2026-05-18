package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.VagaRequestDTO;
import com.matchvagas.backend.dto.VagaResponseDTO;
import com.matchvagas.backend.entity.*;
import com.matchvagas.backend.entity.Usuarios.TipoUsuario;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.VagasMapper;
import com.matchvagas.backend.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

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

    // ID do usuário autenticado nos testes
    private static final Long USUARIO_ID = 10L;
    private static final Long EMPRESA_ID = 1L;

    @BeforeEach
    void setUp() {
        // Montar entidades de lookup
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

        Estado estado = new Estado();
        estado.setId(1L);
        estado.setNome("São Paulo");
        estado.setUf("SP");

        cidade = new Cidade();
        cidade.setId(1L);
        cidade.setNome("São Paulo");
        cidade.setEstado(estado);

        // Empresa vinculada ao usuário autenticado
        Usuarios usuario = new Usuarios();
        usuario.setId(USUARIO_ID);
        usuario.setNome("Gestor Tech Corp");
        usuario.setTipoUsuario(TipoUsuario.EMPRESA);

        empresa = new Empresas();
        empresa.setId(EMPRESA_ID);
        empresa.setNomeFantasia("Tech Corp");
        empresa.setUsuario(usuario);
        empresa.setStatus(Empresas.StatusEmpresa.APROVADA);

        // Vaga completa
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

        // DTO de request — empresaId é ignorado para EMPRESA (busca pelo usuário autenticado)
        request = new VagaRequestDTO(
                null, "Dev Java Pleno",
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

        // DTO de response atualizado com id e empresaId
        responseDTO = new VagaResponseDTO(
                1L,                     // id — ADICIONADO
                "Tech Corp",
                EMPRESA_ID,             // empresaId — ADICIONADO
                "Dev Java Pleno",
                "Vaga para desenvolvedor Java com experiência em Spring Boot",
                "Java 17, Spring Boot, JPA",
                "Vale refeição, Plano de saúde",
                1L, "CLT",
                1L, "Remoto",
                new BigDecimal("5000.00"), new BigDecimal("8000.00"),
                "40h/semana", 18, 40,
                1L, "Ensino Superior",
                "Tecnologia",
                LocalDateTime.now(), LocalDateTime.now().plusDays(30),
                1L, "ATIVA", 3,
                1L, "São Paulo", "SP"
        );
    }

    @AfterEach
    void tearDown() {
        // Limpar o SecurityContext após cada teste
        SecurityContextHolder.clearContext();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Método auxiliar — simula usuário autenticado no SecurityContext
    // ─────────────────────────────────────────────────────────────────────────

    private void autenticarComoEmpresa() {
        autenticar(TipoUsuario.EMPRESA);
    }

    private void autenticarComoAdmin() {
        autenticar(TipoUsuario.ADMIN);
    }

    private void autenticar(TipoUsuario tipo) {
        var auth = new UsernamePasswordAuthenticationToken(
                String.valueOf(USUARIO_ID),
                null,
                List.of(new SimpleGrantedAuthority(tipo.name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RF005 — Cadastro de Vagas
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RF005 — Cadastro de Vagas")
    class RF005 {

        @Test
        @DisplayName("Usuário EMPRESA deve cadastrar vaga na própria empresa automaticamente")
        void deveCadastrarVagaViaUsuarioEmpresa() {
            autenticarComoEmpresa();

            when(vagasMapper.toEntity(request)).thenReturn(vaga);
            when(empresaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(empresa));
            when(tipoVagaRepository.findById(1L)).thenReturn(Optional.of(tipoVaga));
            when(modalidadeRepository.findById(1L)).thenReturn(Optional.of(modalidade));
            when(escolaridadeRepository.findById(1L)).thenReturn(Optional.of(escolaridade));
            when(statusVagaRepository.findById(1L)).thenReturn(Optional.of(statusVaga));
            when(cidadeRepository.findById(1L)).thenReturn(Optional.of(cidade));
            when(vagaRepository.save(any())).thenReturn(vaga);
            when(vagasMapper.toDTO(vaga)).thenReturn(responseDTO);

            VagaResponseDTO result = vagaService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.nomeFantasiaEmpresa()).isEqualTo("Tech Corp");
            assertThat(result.empresaId()).isEqualTo(EMPRESA_ID);
            // Garante que buscou pela empresa do usuário, não por ID direto
            verify(empresaRepository).findByUsuarioId(USUARIO_ID);
            verify(empresaRepository, never()).findById(any());
            verify(vagaRepository).save(any());
        }

        @Test
        @DisplayName("ADMIN deve cadastrar vaga informando empresaId no request")
        void deveCadastrarVagaComoAdmin() {
            autenticarComoAdmin();

            VagaRequestDTO requestAdmin = new VagaRequestDTO(
                    EMPRESA_ID, "Dev Java Pleno",
                    "Descrição", "Requisitos",
                    1L, 1L,
                    new BigDecimal("5000.00"), new BigDecimal("8000.00"),
                    null, "40h/semana", 18, 40, 1L, "TI",
                    LocalDateTime.now().plusDays(30), 1L, 1, 1L
            );

            when(vagasMapper.toEntity(requestAdmin)).thenReturn(vaga);
            when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
            when(tipoVagaRepository.findById(1L)).thenReturn(Optional.of(tipoVaga));
            when(modalidadeRepository.findById(1L)).thenReturn(Optional.of(modalidade));
            when(escolaridadeRepository.findById(1L)).thenReturn(Optional.of(escolaridade));
            when(statusVagaRepository.findById(1L)).thenReturn(Optional.of(statusVaga));
            when(cidadeRepository.findById(1L)).thenReturn(Optional.of(cidade));
            when(vagaRepository.save(any())).thenReturn(vaga);
            when(vagasMapper.toDTO(vaga)).thenReturn(responseDTO);

            VagaResponseDTO result = vagaService.create(requestAdmin);

            assertThat(result).isNotNull();
            // ADMIN usa findById, não findByUsuarioId
            verify(empresaRepository).findById(EMPRESA_ID);
            verify(empresaRepository, never()).findByUsuarioId(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando EMPRESA não tem empresa cadastrada")
        void deveLancarExcecaoEmpresaNaoVinculada() {
            autenticarComoEmpresa();

            when(vagasMapper.toEntity(any())).thenReturn(vaga);
            when(empresaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vagaService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Nenhuma empresa vinculada");

            verify(vagaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando salário mínimo maior que máximo")
        void deveLancarExcecaoSalarioInvalido() {
            autenticarComoEmpresa();

            VagaRequestDTO requestInvalido = new VagaRequestDTO(
                    null, "Dev Java", "Descrição", "Requisitos",
                    1L, 1L,
                    new BigDecimal("9000.00"), new BigDecimal("5000.00"), // mínimo > máximo
                    null, "40h/semana", 18, 40, 1L, "TI",
                    LocalDateTime.now().plusDays(30), 1L, 1, 1L
            );

            when(vagasMapper.toEntity(any())).thenReturn(vaga);
            when(empresaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(empresa));
            when(tipoVagaRepository.findById(1L)).thenReturn(Optional.of(tipoVaga));
            when(modalidadeRepository.findById(1L)).thenReturn(Optional.of(modalidade));
            when(escolaridadeRepository.findById(1L)).thenReturn(Optional.of(escolaridade));
            when(statusVagaRepository.findById(1L)).thenReturn(Optional.of(statusVaga));
            when(cidadeRepository.findById(1L)).thenReturn(Optional.of(cidade));

            assertThatThrownBy(() -> vagaService.create(requestInvalido))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Salário mínimo");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RF006 — Manutenção de Vagas
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RF006 — Manutenção de Vagas")
    class RF006 {

        @Test
        @DisplayName("Empresa deve atualizar sua própria vaga com sucesso")
        void deveAtualizarVagaPropriaEmpresa() {
            autenticarComoEmpresa();

            VagaResponseDTO responseAtualizado = new VagaResponseDTO(
                    1L, "Tech Corp", EMPRESA_ID,
                    "Dev Java Sênior", "Vaga atualizada", "Java 21",
                    null, 1L, "CLT", 1L, "Remoto",
                    new BigDecimal("8000.00"), new BigDecimal("12000.00"),
                    "40h/semana", 25, 45,
                    1L, "Ensino Superior", "Tecnologia",
                    LocalDateTime.now(), LocalDateTime.now().plusDays(30),
                    1L, "ATIVA", 2,
                    1L, "São Paulo", "SP"
            );

            when(vagaRepository.findById(1L)).thenReturn(Optional.of(vaga));
            when(empresaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(empresa));
            when(tipoVagaRepository.findById(1L)).thenReturn(Optional.of(tipoVaga));
            when(modalidadeRepository.findById(1L)).thenReturn(Optional.of(modalidade));
            when(escolaridadeRepository.findById(1L)).thenReturn(Optional.of(escolaridade));
            when(statusVagaRepository.findById(1L)).thenReturn(Optional.of(statusVaga));
            when(cidadeRepository.findById(1L)).thenReturn(Optional.of(cidade));
            when(vagaRepository.save(any())).thenReturn(vaga);
            when(vagasMapper.toDTO(any())).thenReturn(responseAtualizado);

            VagaResponseDTO result = vagaService.update(1L, request);

            assertThat(result.titulo()).isEqualTo("Dev Java Sênior");
            verify(vagaRepository).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando EMPRESA tenta editar vaga de outra empresa")
        void deveLancarExcecaoEditarVagaDeOutraEmpresa() {
            autenticarComoEmpresa();

            // Empresa diferente vinculada à vaga
            Empresas outraEmpresa = new Empresas();
            outraEmpresa.setId(99L);
            outraEmpresa.setNomeFantasia("Outra Empresa");
            vaga.setEmpresas(outraEmpresa);

            when(vagaRepository.findById(1L)).thenReturn(Optional.of(vaga));
            // Retorna a empresa do usuário autenticado (diferente da vaga)
            when(empresaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(empresa));

            assertThatThrownBy(() -> vagaService.update(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("permissão");
        }

        @Test
        @DisplayName("ADMIN deve atualizar qualquer vaga sem restrição")
        void adminDeveAtualizarQualquerVaga() {
            autenticarComoAdmin();

            when(vagaRepository.findById(1L)).thenReturn(Optional.of(vaga));
            when(tipoVagaRepository.findById(1L)).thenReturn(Optional.of(tipoVaga));
            when(modalidadeRepository.findById(1L)).thenReturn(Optional.of(modalidade));
            when(escolaridadeRepository.findById(1L)).thenReturn(Optional.of(escolaridade));
            when(statusVagaRepository.findById(1L)).thenReturn(Optional.of(statusVaga));
            when(cidadeRepository.findById(1L)).thenReturn(Optional.of(cidade));
            when(vagaRepository.save(any())).thenReturn(vaga);
            when(vagasMapper.toDTO(any())).thenReturn(responseDTO);

            VagaResponseDTO result = vagaService.update(1L, request);

            assertThat(result).isNotNull();
            // ADMIN não verifica empresa do usuário
            verify(empresaRepository, never()).findByUsuarioId(any());
        }

        @Test
        @DisplayName("Empresa deve remover sua própria vaga")
        void deveRemoverVagaPropriaEmpresa() {
            autenticarComoEmpresa();

            when(vagaRepository.findById(1L)).thenReturn(Optional.of(vaga));
            when(empresaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(empresa));
            doNothing().when(vagaRepository).deleteById(1L);

            vagaService.delete(1L);

            verify(vagaRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Deve lançar exceção ao remover vaga inexistente")
        void deveLancarExcecaoRemoverVagaInexistente() {
            autenticarComoEmpresa();

            when(vagaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vagaService.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(vagaRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar vaga inexistente")
        void deveLancarExcecaoAtualizarVagaInexistente() {
            autenticarComoEmpresa();

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
            when(vagaRepository.searchComFiltros(null, null, null, null, null)).thenReturn(List.of(vaga));
            when(vagasMapper.toDTO(vaga)).thenReturn(responseDTO);

            List<VagaResponseDTO> result = vagaService.search(null, null, null, null, null);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Deve filtrar vagas por título")
        void deveFiltrarPorTitulo() {
            when(vagaRepository.searchComFiltros("Java", null, null, null, null)).thenReturn(List.of(vaga));
            when(vagasMapper.toDTO(vaga)).thenReturn(responseDTO);

            List<VagaResponseDTO> result = vagaService.search("Java", null, null, null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).titulo()).isEqualTo("Dev Java Pleno");
        }

        @Test
        @DisplayName("Deve filtrar vagas por área de atuação")
        void deveFiltrarPorAreaAtuacao() {
            when(vagaRepository.searchComFiltros(null, "Tecnologia", null, null, null)).thenReturn(List.of(vaga));
            when(vagasMapper.toDTO(vaga)).thenReturn(responseDTO);

            List<VagaResponseDTO> result = vagaService.search(null, "Tecnologia", null, null, null);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando nenhuma vaga bate com o filtro")
        void deveRetornarVazioQuandoSemCorrespondencia() {
            when(vagaRepository.searchComFiltros("Python", null, null, null, null)).thenReturn(List.of());

            List<VagaResponseDTO> result = vagaService.search("Python", null, null, null, null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Deve buscar vaga por ID")
        void deveBuscarPorId() {
            when(vagaRepository.findById(1L)).thenReturn(Optional.of(vaga));
            when(vagasMapper.toDTO(vaga)).thenReturn(responseDTO);

            VagaResponseDTO result = vagaService.findById(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.titulo()).isEqualTo("Dev Java Pleno");
        }

        @Test
        @DisplayName("Deve lançar exceção para vaga não encontrada por ID")
        void deveLancarExcecaoVagaNaoEncontrada() {
            when(vagaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vagaService.findById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Deve listar vagas da empresa do usuário autenticado")
        void deveListarMinhasVagas() {
            autenticarComoEmpresa();

            when(empresaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(empresa));
            when(vagaRepository.findByEmpresasId(EMPRESA_ID)).thenReturn(List.of(vaga));
            when(vagasMapper.toDTO(vaga)).thenReturn(responseDTO);

            List<VagaResponseDTO> result = vagaService.findMinhasVagas();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).empresaId()).isEqualTo(EMPRESA_ID);
        }

        @Test
        @DisplayName("Deve lançar exceção em findMinhasVagas quando empresa não vinculada")
        void deveLancarExcecaoMinhasVagasSemEmpresa() {
            autenticarComoEmpresa();

            when(empresaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vagaService.findMinhasVagas())
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Nenhuma empresa vinculada");
        }
    }
}
