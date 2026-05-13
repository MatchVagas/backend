package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.CandidatoRequestDTO;
import com.matchvagas.backend.dto.CandidatoResponseDTO;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.entity.Usuarios.TipoUsuario;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.CandidatoMapper;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.UsuariosRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RF003 — Gerenciamento de Perfil do Candidato")
class CandidatoServiceTest {

    @Mock CandidatoRepository candidatoRepository;
    @Mock UsuariosRepository usuariosRepository;
    @Mock CandidatoMapper candidatoMapper;
    @Mock com.matchvagas.backend.repository.TelefoneRepository telefoneRepository;
    @Mock com.matchvagas.backend.repository.TipoTelefoneRepository tipoTelefoneRepository;

    @InjectMocks CandidatoService candidatoService;

    private Usuarios usuario;
    private Candidatos candidato;
    private CandidatoRequestDTO request;
    private CandidatoResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        usuario = new Usuarios();
        usuario.setId(1L);
        usuario.setNome("Maria Oliveira");
        usuario.setEmail("maria@email.com");
        usuario.setTipoUsuario(TipoUsuario.CANDIDATO);

        candidato = new Candidatos();
        candidato.setId(10L);
        candidato.setCpf("123.456.789-00");
        candidato.setUsuario(usuario);
        candidato.setObjetivoProfissional("Desenvolvedor Java Pleno");
        candidato.setDisponibilidade("Imediata");
        candidato.setPretensaoSalarial(new BigDecimal("5000.00"));

        request = new CandidatoRequestDTO(
                "123.456.789-00",
                null,
                null,
                null,
                "Desenvolvedor Java Pleno",
                "Imediata",
                new BigDecimal("5000.00"),
                null,
                null
        );

        responseDTO = new CandidatoResponseDTO(
                10L, "Maria Oliveira", "maria@email.com",
                null, "123.456.789-00", "Desenvolvedor Java Pleno",
                "Imediata", new BigDecimal("5000.00"),
                null, null
        );
    }

    @Nested
    @DisplayName("Visualizar perfil")
    class VisualizarPerfil {

        @Test
        @DisplayName("Deve retornar perfil do candidato pelo ID do usuário")
        void deveRetornarPerfilPorUsuarioId() {
            when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));
            when(candidatoMapper.toResponseDTO(candidato)).thenReturn(responseDTO);

            CandidatoResponseDTO result = candidatoService.findByUsuarioId(1L);

            assertThat(result).isNotNull();
            assertThat(result.nome()).isEqualTo("Maria Oliveira");
            assertThat(result.cpf()).isEqualTo("123.456.789-00");
        }

        @Test
        @DisplayName("Deve lançar exceção quando perfil não encontrado")
        void deveLancarExcecaoPerfilNaoEncontrado() {
            when(candidatoRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> candidatoService.findByUsuarioId(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Criar perfil")
    class CriarPerfil {

        @Test
        @DisplayName("Deve criar perfil de candidato com sucesso")
        void deveCriarPerfilComSucesso() {
            when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());
            when(usuariosRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(candidatoMapper.toEntity(request)).thenReturn(candidato);
            when(candidatoRepository.save(any())).thenReturn(candidato);
            when(candidatoMapper.toResponseDTO(candidato)).thenReturn(responseDTO);

            CandidatoResponseDTO result = candidatoService.create(1L, request);

            assertThat(result).isNotNull();
            assertThat(result.objetivoProfissional()).isEqualTo("Desenvolvedor Java Pleno");
            verify(candidatoRepository).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando já existe perfil para o usuário")
        void deveLancarExcecaoPerfilJaExistente() {
            when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));

            assertThatThrownBy(() -> candidatoService.create(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("perfil");

            verify(candidatoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando usuário não encontrado")
        void deveLancarExcecaoUsuarioNaoEncontrado() {
            when(candidatoRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());
            when(usuariosRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> candidatoService.create(99L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Atualizar perfil")
    class AtualizarPerfil {

        @Test
        @DisplayName("Deve atualizar objetivo profissional e disponibilidade")
        void deveAtualizarPerfil() {
            CandidatoRequestDTO requestAtualizado = new CandidatoRequestDTO(
                    null, null, null, null, "Arquiteto de Software", "30 dias", new BigDecimal("8000.00"), null, null);

            CandidatoResponseDTO responseAtualizado = new CandidatoResponseDTO(
                    10L, "Maria Oliveira", "maria@email.com",
                    null, "123.456.789-00", "Arquiteto de Software",
                    "30 dias", new BigDecimal("8000.00"), null, null);

            when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));
            when(candidatoRepository.save(any())).thenReturn(candidato);
            when(candidatoMapper.toResponseDTO(any())).thenReturn(responseAtualizado);

            CandidatoResponseDTO result = candidatoService.update(1L, requestAtualizado);

            assertThat(result.objetivoProfissional()).isEqualTo("Arquiteto de Software");
            assertThat(result.pretensaoSalarial()).isEqualByComparingTo("8000.00");
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar perfil inexistente")
        void deveLancarExcecaoAoAtualizarPerfilInexistente() {
            when(candidatoRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> candidatoService.update(99L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
