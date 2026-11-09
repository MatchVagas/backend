package com.matchvagas.backend.service;

import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Curriculos;
import com.matchvagas.backend.entity.Habilidade;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.entity.Usuarios.TipoUsuario;
import com.matchvagas.backend.entity.CandidatoEmbedding;
import com.matchvagas.backend.entity.Experiencia;
import com.matchvagas.backend.entity.Formacao;
import com.matchvagas.backend.repository.CandidatoEmbeddingRepository;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.CurriculoRepository;
import com.matchvagas.backend.repository.ExperienciaRepository;
import com.matchvagas.backend.repository.FormacaoRepository;
import com.matchvagas.backend.repository.PasswordResetTokenRepository;
import com.matchvagas.backend.repository.UsuariosRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LGPD-09 — retenção e pseudoanonimização de dados")
class RetencaoDadosServiceTest {

    @Mock UsuariosRepository usuariosRepository;
    @Mock CandidatoRepository candidatoRepository;
    @Mock CurriculoRepository curriculoRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock CurriculoService curriculoService;
    @Mock FotoPerfilService fotoPerfilService;
    @Mock ExperienciaRepository experienciaRepository;
    @Mock FormacaoRepository formacaoRepository;
    @Mock CandidatoEmbeddingRepository candidatoEmbeddingRepository;
    @InjectMocks RetencaoDadosService service;

    private Usuarios usuario;
    private Candidatos candidato;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "habilitado", true);
        ReflectionTestUtils.setField(service, "inatividadeAnos", 2);
        ReflectionTestUtils.setField(service, "curriculoInatividadeAnos", 1);

        usuario = new Usuarios();
        usuario.setId(1L);
        usuario.setNome("Ana Souza");
        usuario.setEmail("ana@x.com");
        usuario.setAtivo(true);
        usuario.setTelefones(new ArrayList<>());

        Habilidade java = new Habilidade();
        java.setNome("Java");
        candidato = new Candidatos();
        candidato.setId(10L);
        candidato.setUsuario(usuario);
        candidato.setObjetivoProfissional("Backend");
        candidato.setPretensaoSalarial(new BigDecimal("5000"));
        candidato.setFotoPerfilUrl("https://foto");
        candidato.setHabilidades(new ArrayList<>(List.of(java)));
        candidato.setDisponivelParaRecomendacoes(true);
    }

    @Test
    @DisplayName("desligado não faz nada")
    void desligado() {
        ReflectionTestUtils.setField(service, "habilitado", false);

        service.executar();

        verifyNoInteractions(usuariosRepository, passwordResetTokenRepository, curriculoService);
    }

    @Test
    @DisplayName("pseudoanonimiza candidato inativo: dados identificadores, perfil e foto")
    void pseudoanonimiza() {
        when(usuariosRepository.findByTipoUsuarioAndAtivoAndDataUltimoAcessoBefore(eq(TipoUsuario.CANDIDATO), eq(true), any()))
                .thenReturn(List.of())
                .thenReturn(List.of(usuario));
        when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));

        service.executar();

        verify(passwordResetTokenRepository).deleteExpiredAndUsed(any());
        verify(fotoPerfilService).deletarCandidato(1L);
        assertThat(usuario.getNome()).isEqualTo("(conta removida)");
        assertThat(usuario.getEmail()).isEqualTo("removido-1@anonimizado.local");
        assertThat(usuario.getAtivo()).isFalse();
        assertThat(candidato.getObjetivoProfissional()).isNull();
        assertThat(candidato.getPretensaoSalarial()).isNull();
        assertThat(candidato.getEndereco()).isNull();
        assertThat(candidato.getHabilidades()).isEmpty();
        verify(candidatoRepository).save(candidato);
        verify(usuariosRepository).save(usuario);
    }

    @Test
    @DisplayName("pseudoanonimização remove experiências, formações, vetor e sai das recomendações")
    void removeHistoricoEVetor() {
        List<Experiencia> experiencias = List.of(new Experiencia());
        List<Formacao> formacoes = List.of(new Formacao());
        CandidatoEmbedding vetor = new CandidatoEmbedding();
        when(usuariosRepository.findByTipoUsuarioAndAtivoAndDataUltimoAcessoBefore(eq(TipoUsuario.CANDIDATO), eq(true), any()))
                .thenReturn(List.of())
                .thenReturn(List.of(usuario));
        when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));
        when(experienciaRepository.findByCandidatoId(10L)).thenReturn(experiencias);
        when(formacaoRepository.findByCandidatoId(10L)).thenReturn(formacoes);
        when(candidatoEmbeddingRepository.findByCandidatoId(10L)).thenReturn(Optional.of(vetor));

        service.executar();

        verify(experienciaRepository).deleteAll(experiencias);
        verify(formacaoRepository).deleteAll(formacoes);
        verify(candidatoEmbeddingRepository).delete(vetor);
        assertThat(candidato.isDisponivelParaRecomendacoes()).isFalse();
        assertThat(candidato.getRecomendacoesRevogacaoEm()).isNotNull();
    }

    @Test
    @DisplayName("remove o currículo de candidato inativo há mais de um ano")
    void removeCurriculo() {
        when(usuariosRepository.findByTipoUsuarioAndAtivoAndDataUltimoAcessoBefore(eq(TipoUsuario.CANDIDATO), eq(true), any()))
                .thenReturn(List.of(usuario))
                .thenReturn(List.of());
        when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));
        when(curriculoRepository.findByCandidatoId(10L)).thenReturn(Optional.of(new Curriculos()));

        service.executar();

        verify(curriculoService).deletar(1L);
    }

    @Test
    @DisplayName("falha ao remover foto não impede a pseudoanonimização")
    void falhaNaFoto() {
        when(usuariosRepository.findByTipoUsuarioAndAtivoAndDataUltimoAcessoBefore(eq(TipoUsuario.CANDIDATO), eq(true), any()))
                .thenReturn(List.of())
                .thenReturn(List.of(usuario));
        when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));
        doThrow(new RuntimeException("storage fora")).when(fotoPerfilService).deletarCandidato(1L);

        service.executar();

        assertThat(usuario.getAtivo()).isFalse();
        verify(usuariosRepository).save(usuario);
    }
}
