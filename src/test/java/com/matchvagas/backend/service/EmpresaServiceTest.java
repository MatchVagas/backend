package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.EmpresaRequestDTO;
import com.matchvagas.backend.dto.EmpresaResponseDTO;
import com.matchvagas.backend.entity.Empresas;
import com.matchvagas.backend.entity.Porte;
import com.matchvagas.backend.entity.RamoAtuacao;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.EmpresaMapper;
import com.matchvagas.backend.repository.EmpresaRepository;
import com.matchvagas.backend.repository.PorteRepository;
import com.matchvagas.backend.repository.RamoAtuacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RF004 — Gerenciamento de Perfil da Empresa")
class EmpresaServiceTest {

    @Mock EmpresaRepository empresaRepository;
    @Mock PorteRepository porteRepository;
    @Mock RamoAtuacaoRepository ramoAtuacaoRepository;
    @Mock EmpresaMapper empresaMapper;

    @InjectMocks EmpresaService empresaService;

    private Empresas empresa;
    private EmpresaRequestDTO request;
    private EmpresaResponseDTO responseDTO;
    private Porte porte;
    private RamoAtuacao ramo;

    @BeforeEach
    void setUp() {
        porte = new Porte();
        porte.setId(1L);
        porte.setDescricao("Médio Porte");

        ramo = new RamoAtuacao();
        ramo.setId(2L);
        ramo.setDescricao("Tecnologia");

        empresa = new Empresas();
        empresa.setId(1L);
        empresa.setCnpj("12.345.678/0001-90");
        empresa.setRazaoSocial("Tech Corp Ltda");
        empresa.setNomeFantasia("Tech Corp");
        empresa.setDescricao("Empresa de tecnologia");
        empresa.setPorte(porte);
        empresa.setRamoAtuacao(ramo);
        empresa.setSite("https://techcorp.com");

        request = new EmpresaRequestDTO(
                "12.345.678/0001-90",
                "Tech Corp Ltda",
                "Tech Corp",
                "Empresa de tecnologia",
                1L,
                2L,
                "https://techcorp.com"
        );

        responseDTO = new EmpresaResponseDTO(
                1L, "12.345.678/0001-90", "Tech Corp Ltda", "Tech Corp",
                "Empresa de tecnologia", "Médio Porte", "Tecnologia",
                "https://techcorp.com", null
        );
    }

    @Nested
    @DisplayName("Cadastrar empresa")
    class CadastrarEmpresa {

        @Test
        @DisplayName("Deve cadastrar empresa com sucesso")
        void deveCadastrarEmpresaComSucesso() {
            when(empresaRepository.findByCnpjContainingIgnoreCase("12.345.678/0001-90"))
                    .thenReturn(Optional.empty());
            when(empresaMapper.toEntity(request)).thenReturn(empresa);
            when(porteRepository.findById(1L)).thenReturn(Optional.of(porte));
            when(ramoAtuacaoRepository.findById(2L)).thenReturn(Optional.of(ramo));
            when(empresaRepository.save(any())).thenReturn(empresa);
            when(empresaMapper.toResponseDTO(empresa)).thenReturn(responseDTO);

            EmpresaResponseDTO result = empresaService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.nomeFantasia()).isEqualTo("Tech Corp");
            assertThat(result.porte()).isEqualTo("Médio Porte");
            assertThat(result.ramoAtuacao()).isEqualTo("Tecnologia");
            verify(empresaRepository).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção para CNPJ já cadastrado")
        void deveLancarExcecaoCnpjDuplicado() {
            when(empresaRepository.findByCnpjContainingIgnoreCase("12.345.678/0001-90"))
                    .thenReturn(Optional.of(empresa));

            assertThatThrownBy(() -> empresaService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CNPJ");

            verify(empresaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando porte não encontrado")
        void deveLancarExcecaoPorteNaoEncontrado() {
            when(empresaRepository.findByCnpjContainingIgnoreCase(anyString()))
                    .thenReturn(Optional.empty());
            when(empresaMapper.toEntity(any())).thenReturn(empresa);
            when(porteRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> empresaService.create(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Porte");
        }

        @Test
        @DisplayName("Deve lançar exceção quando ramo não encontrado")
        void deveLancarExcecaoRamoNaoEncontrado() {
            when(empresaRepository.findByCnpjContainingIgnoreCase(anyString()))
                    .thenReturn(Optional.empty());
            when(empresaMapper.toEntity(any())).thenReturn(empresa);
            when(porteRepository.findById(1L)).thenReturn(Optional.of(porte));
            when(ramoAtuacaoRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> empresaService.create(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Ramo");
        }
    }

    @Nested
    @DisplayName("Atualizar empresa")
    class AtualizarEmpresa {

        @Test
        @DisplayName("Deve atualizar dados da empresa com sucesso")
        void deveAtualizarEmpresaComSucesso() {
            EmpresaRequestDTO requestAtualizado = new EmpresaRequestDTO(
                    "12.345.678/0001-90", "Tech Corp Ltda", "TechCorp 2.0",
                    "Nova descrição", 1L, 2L, "https://techcorp2.com");

            EmpresaResponseDTO responseAtualizado = new EmpresaResponseDTO(
                    1L, "12.345.678/0001-90", "Tech Corp Ltda", "TechCorp 2.0",
                    "Nova descrição", "Médio Porte", "Tecnologia",
                    "https://techcorp2.com", null);

            when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
            when(porteRepository.findById(1L)).thenReturn(Optional.of(porte));
            when(ramoAtuacaoRepository.findById(2L)).thenReturn(Optional.of(ramo));
            when(empresaRepository.save(any())).thenReturn(empresa);
            when(empresaMapper.toResponseDTO(any())).thenReturn(responseAtualizado);

            EmpresaResponseDTO result = empresaService.update(1L, requestAtualizado);

            assertThat(result.nomeFantasia()).isEqualTo("TechCorp 2.0");
            assertThat(result.site()).isEqualTo("https://techcorp2.com");
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar empresa inexistente")
        void deveLancarExcecaoEmpresaNaoEncontrada() {
            when(empresaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> empresaService.update(99L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Listar empresas")
    class ListarEmpresas {

        @Test
        @DisplayName("Deve retornar lista de todas as empresas")
        void deveRetornarListaDeEmpresas() {
            when(empresaRepository.findAll()).thenReturn(List.of(empresa));
            when(empresaMapper.toResponseDTO(empresa)).thenReturn(responseDTO);

            List<EmpresaResponseDTO> result = empresaService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).nomeFantasia()).isEqualTo("Tech Corp");
        }
    }
}
