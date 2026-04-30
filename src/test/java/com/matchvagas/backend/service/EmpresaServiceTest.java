package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.EmpresaRequestDTO;
import com.matchvagas.backend.dto.EmpresaResponseDTO;
import com.matchvagas.backend.entity.Empresas;
import com.matchvagas.backend.entity.Porte;
import com.matchvagas.backend.entity.RamoAtuacao;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.entity.Usuarios.TipoUsuario;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.EmpresaMapper;
import com.matchvagas.backend.repository.EmpresaRepository;
import com.matchvagas.backend.repository.PorteRepository;
import com.matchvagas.backend.repository.RamoAtuacaoRepository;
import com.matchvagas.backend.repository.UsuariosRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RF004 — Gerenciamento de Perfil da Empresa")
class EmpresaServiceTest {

    @Mock EmpresaRepository empresaRepository;
    @Mock PorteRepository porteRepository;
    @Mock RamoAtuacaoRepository ramoAtuacaoRepository;
    @Mock UsuariosRepository usuariosRepository; // ADICIONADO — EmpresaService agora injeta UsuariosRepository
    @Mock EmpresaMapper empresaMapper;

    @InjectMocks EmpresaService empresaService;

    private Empresas empresa;
    private EmpresaRequestDTO request;
    private EmpresaResponseDTO responseDTO;
    private Porte porte;
    private RamoAtuacao ramo;
    private Usuarios usuario;

    private static final Long USUARIO_ID = 10L;

    @BeforeEach
    void setUp() {
        porte = new Porte();
        porte.setId(1L);
        porte.setDescricao("Médio Porte");

        ramo = new RamoAtuacao();
        ramo.setId(2L);
        ramo.setDescricao("Tecnologia");

        // Usuário gestor da empresa
        usuario = new Usuarios();
        usuario.setId(USUARIO_ID);
        usuario.setNome("Gestor Tech Corp");
        usuario.setEmail("gestor@techcorp.com");
        usuario.setTipoUsuario(TipoUsuario.EMPRESA);

        empresa = new Empresas();
        empresa.setId(1L);
        empresa.setCnpj("12.345.678/0001-90");
        empresa.setRazaoSocial("Tech Corp Ltda");
        empresa.setNomeFantasia("Tech Corp");
        empresa.setDescricao("Empresa de tecnologia");
        empresa.setPorte(porte);
        empresa.setRamoAtuacao(ramo);
        empresa.setSite("https://techcorp.com");
        empresa.setUsuario(usuario); // ADICIONADO — vínculo com gestor

        // ATUALIZADO — EmpresaRequestDTO agora tem usuarioGestorId
        request = new EmpresaRequestDTO(
                "12.345.678/0001-90",
                "Tech Corp Ltda",
                "Tech Corp",
                "Empresa de tecnologia",
                1L,
                2L,
                "https://techcorp.com",
                null // usuarioGestorId — null para EMPRESA (vincula automaticamente)
        );

        // ATUALIZADO — EmpresaResponseDTO agora tem usuarioGestorId e nomeGestor
        responseDTO = new EmpresaResponseDTO(
                1L, "12.345.678/0001-90", "Tech Corp Ltda", "Tech Corp",
                "Empresa de tecnologia", "Médio Porte", "Tecnologia",
                "https://techcorp.com", null,
                USUARIO_ID,          // usuarioGestorId — ADICIONADO
                "Gestor Tech Corp"   // nomeGestor — ADICIONADO
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // Método auxiliar para autenticar como EMPRESA
    private void autenticarComoEmpresa() {
        var auth = new UsernamePasswordAuthenticationToken(
                String.valueOf(USUARIO_ID), null,
                List.of(new SimpleGrantedAuthority(TipoUsuario.EMPRESA.name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // Método auxiliar para autenticar como ADMIN
    private void autenticarComoAdmin() {
        var auth = new UsernamePasswordAuthenticationToken(
                String.valueOf(USUARIO_ID), null,
                List.of(new SimpleGrantedAuthority(TipoUsuario.ADMIN.name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("Cadastrar empresa")
    class CadastrarEmpresa {

        @Test
        @DisplayName("Usuário EMPRESA deve cadastrar empresa vinculada a si mesmo")
        void deveCadastrarEmpresaComoEmpresa() {
            autenticarComoEmpresa();

            when(empresaRepository.findByCnpjContainingIgnoreCase("12.345.678/0001-90"))
                    .thenReturn(Optional.empty());
            when(empresaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());
            when(usuariosRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(empresaMapper.toEntity(request)).thenReturn(empresa);
            when(porteRepository.findById(1L)).thenReturn(Optional.of(porte));
            when(ramoAtuacaoRepository.findById(2L)).thenReturn(Optional.of(ramo));
            when(empresaRepository.save(any())).thenReturn(empresa);
            when(empresaMapper.toResponseDTO(empresa)).thenReturn(responseDTO);

            EmpresaResponseDTO result = empresaService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.nomeFantasia()).isEqualTo("Tech Corp");
            assertThat(result.usuarioGestorId()).isEqualTo(USUARIO_ID);
            assertThat(result.nomeGestor()).isEqualTo("Gestor Tech Corp");
            verify(empresaRepository).save(any());
        }

        @Test
        @DisplayName("ADMIN deve cadastrar empresa para outro usuário via usuarioGestorId")
        void deveCadastrarEmpresaComoAdmin() {
            autenticarComoAdmin();

            Long outroUsuarioId = 99L;
            Usuarios outroUsuario = new Usuarios();
            outroUsuario.setId(outroUsuarioId);
            outroUsuario.setTipoUsuario(TipoUsuario.EMPRESA);

            EmpresaRequestDTO requestAdmin = new EmpresaRequestDTO(
                    "12.345.678/0001-90", "Tech Corp Ltda", "Tech Corp",
                    "Empresa de tecnologia", 1L, 2L, "https://techcorp.com",
                    outroUsuarioId // ADMIN informa o usuário gestor
            );

            when(empresaRepository.findByCnpjContainingIgnoreCase(anyString()))
                    .thenReturn(Optional.empty());
            when(usuariosRepository.findById(outroUsuarioId)).thenReturn(Optional.of(outroUsuario));
            when(empresaMapper.toEntity(requestAdmin)).thenReturn(empresa);
            when(porteRepository.findById(1L)).thenReturn(Optional.of(porte));
            when(ramoAtuacaoRepository.findById(2L)).thenReturn(Optional.of(ramo));
            when(empresaRepository.save(any())).thenReturn(empresa);
            when(empresaMapper.toResponseDTO(any())).thenReturn(responseDTO);

            EmpresaResponseDTO result = empresaService.create(requestAdmin);

            assertThat(result).isNotNull();
            verify(usuariosRepository).findById(outroUsuarioId);
        }

        @Test
        @DisplayName("Deve lançar exceção para CNPJ já cadastrado")
        void deveLancarExcecaoCnpjDuplicado() {
            autenticarComoEmpresa();

            when(empresaRepository.findByCnpjContainingIgnoreCase("12.345.678/0001-90"))
                    .thenReturn(Optional.of(empresa));

            assertThatThrownBy(() -> empresaService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CNPJ");

            verify(empresaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando usuário EMPRESA já possui empresa cadastrada")
        void deveLancarExcecaoEmpresaJaCadastrada() {
            autenticarComoEmpresa();

            when(empresaRepository.findByCnpjContainingIgnoreCase(anyString()))
                    .thenReturn(Optional.empty());
            when(empresaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(empresa));

            assertThatThrownBy(() -> empresaService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("já possui uma empresa");

            verify(empresaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando porte não encontrado")
        void deveLancarExcecaoPorteNaoEncontrado() {
            autenticarComoEmpresa();

            when(empresaRepository.findByCnpjContainingIgnoreCase(anyString()))
                    .thenReturn(Optional.empty());
            when(empresaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());
            when(usuariosRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(empresaMapper.toEntity(any())).thenReturn(empresa);
            when(porteRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> empresaService.create(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Porte");
        }

        @Test
        @DisplayName("Deve lançar exceção quando ramo não encontrado")
        void deveLancarExcecaoRamoNaoEncontrado() {
            autenticarComoEmpresa();

            when(empresaRepository.findByCnpjContainingIgnoreCase(anyString()))
                    .thenReturn(Optional.empty());
            when(empresaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());
            when(usuariosRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
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
        @DisplayName("Gestor deve atualizar sua própria empresa")
        void deveAtualizarEmpresaComoGestor() {
            autenticarComoEmpresa();

            EmpresaRequestDTO requestAtualizado = new EmpresaRequestDTO(
                    "12.345.678/0001-90", "Tech Corp Ltda", "TechCorp 2.0",
                    "Nova descrição", 1L, 2L, "https://techcorp2.com", null);

            EmpresaResponseDTO responseAtualizado = new EmpresaResponseDTO(
                    1L, "12.345.678/0001-90", "Tech Corp Ltda", "TechCorp 2.0",
                    "Nova descrição", "Médio Porte", "Tecnologia",
                    "https://techcorp2.com", null, USUARIO_ID, "Gestor Tech Corp");

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
        @DisplayName("Deve lançar exceção quando EMPRESA tenta editar empresa de outro usuário")
        void deveLancarExcecaoEditarEmpresaDeOutroUsuario() {
            autenticarComoEmpresa();

            // Empresa pertence a outro usuário
            Usuarios outroUsuario = new Usuarios();
            outroUsuario.setId(99L);
            empresa.setUsuario(outroUsuario);

            when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));

            assertThatThrownBy(() -> empresaService.update(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("permissão");
        }

        @Test
        @DisplayName("ADMIN deve atualizar qualquer empresa sem restrição")
        void adminDeveAtualizarQualquerEmpresa() {
            autenticarComoAdmin();

            when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
            when(porteRepository.findById(1L)).thenReturn(Optional.of(porte));
            when(ramoAtuacaoRepository.findById(2L)).thenReturn(Optional.of(ramo));
            when(empresaRepository.save(any())).thenReturn(empresa);
            when(empresaMapper.toResponseDTO(any())).thenReturn(responseDTO);

            EmpresaResponseDTO result = empresaService.update(1L, request);

            assertThat(result).isNotNull();
            // ADMIN não verifica usuário gestor
            verify(usuariosRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar empresa inexistente")
        void deveLancarExcecaoEmpresaNaoEncontrada() {
            autenticarComoEmpresa();

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
            assertThat(result.get(0).usuarioGestorId()).isEqualTo(USUARIO_ID);
        }

        @Test
        @DisplayName("Deve retornar empresa pelo usuário autenticado")
        void deveRetornarEmpresaDoUsuario() {
            autenticarComoEmpresa();

            when(empresaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(empresa));
            when(empresaMapper.toResponseDTO(empresa)).thenReturn(responseDTO);

            EmpresaResponseDTO result = empresaService.findByUsuarioAutenticado();

            assertThat(result.nomeFantasia()).isEqualTo("Tech Corp");
        }

        @Test
        @DisplayName("Deve lançar exceção quando usuário não tem empresa vinculada")
        void deveLancarExcecaoSemEmpresaVinculada() {
            autenticarComoEmpresa();

            when(empresaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> empresaService.findByUsuarioAutenticado())
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
