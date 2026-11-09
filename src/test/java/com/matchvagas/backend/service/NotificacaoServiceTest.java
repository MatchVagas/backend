package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.NotificacoesRequestDTO;
import com.matchvagas.backend.dto.NotificacoesResponseDTO;
import com.matchvagas.backend.entity.Notificacao;
import com.matchvagas.backend.entity.TipoNotificacao;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.NotificacaoMapper;
import com.matchvagas.backend.repository.NotificacaoRepository;
import com.matchvagas.backend.repository.TipoNotificacaoRepository;
import com.matchvagas.backend.repository.UsuariosRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notificações in-app, push e e-mail")
class NotificacaoServiceTest {

    @Mock NotificacaoRepository notificacaoRepository;
    @Mock UsuariosRepository usuariosRepository;
    @Mock TipoNotificacaoRepository tipoNotificacaoRepository;
    @Mock NotificacaoMapper notificacaoMapper;
    @Mock EmailService emailService;
    @Mock RealtimeService realtimeService;
    @Spy AposCommitExecutor aposCommit = new AposCommitExecutor();
    @InjectMocks NotificacaoService service;

    private Usuarios usuario;
    private final NotificacoesResponseDTO resposta = mock(NotificacoesResponseDTO.class);

    @BeforeEach
    void setUp() {
        usuario = new Usuarios();
        usuario.setId(1L);
        usuario.setNome("Ana");
        usuario.setEmail("ana@x.com");
    }

    private void prepararCriacao() {
        when(usuariosRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(notificacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(notificacaoMapper.toDTO(any())).thenReturn(resposta);
    }

    private Notificacao notificacao(Long donoId) {
        Usuarios dono = new Usuarios();
        dono.setId(donoId);
        Notificacao n = new Notificacao();
        n.setId(5L);
        n.setUsuario(dono);
        n.setLida(false);
        return n;
    }

    @Test
    @DisplayName("cria não lida, envia push e e-mail ao destinatário")
    void criaComPushEEmail() {
        prepararCriacao();

        service.criar(new NotificacoesRequestDTO("Nova vaga", "Confira", 1L, null));

        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(notificacaoRepository).save(captor.capture());
        assertThat(captor.getValue().getLida()).isFalse();
        assertThat(captor.getValue().getDataEnvio()).isNotNull();
        verify(realtimeService).enviarPara(1L, "notificacao", resposta);
        verify(emailService).enviarEmail(eq("ana@x.com"), eq("Nova vaga"), contains("Confira"));
    }

    @Test
    @DisplayName("conteúdo digitado por usuários é escapado no HTML do e-mail")
    void escapaHtmlNoEmail() {
        prepararCriacao();
        usuario.setNome("<b>Ana</b>");

        service.criar(new NotificacoesRequestDTO("Vaga", "<img src=x onerror=alert(1)>", 1L, null));

        ArgumentCaptor<String> corpo = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarEmail(eq("ana@x.com"), eq("Vaga"), corpo.capture());
        assertThat(corpo.getValue())
                .doesNotContain("<img", "<b>Ana")
                .contains("&lt;img src=x onerror=alert(1)&gt;", "&lt;b&gt;Ana&lt;/b&gt;");
    }

    @Test
    @DisplayName("sem e-mail quando suprimido (ex.: chat)")
    void semEmail() {
        prepararCriacao();

        service.criar(new NotificacoesRequestDTO("Msg", "Oi", 1L, null), false);

        verify(realtimeService).enviarPara(1L, "notificacao", resposta);
        verify(emailService, never()).enviarEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("falha no push ou e-mail não desfaz a notificação")
    void falhaDeEntregaNaoPropaga() {
        prepararCriacao();
        doThrow(new RuntimeException("SMTP fora")).when(emailService).enviarEmail(anyString(), anyString(), anyString());

        assertThatCode(() -> service.criar(new NotificacoesRequestDTO("T", "M", 1L, null)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("usuário ou tipo inexistente retornam 404")
    void naoEncontrado() {
        when(usuariosRepository.findById(9L)).thenReturn(Optional.empty());
        when(usuariosRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(tipoNotificacaoRepository.findById(77)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(new NotificacoesRequestDTO("T", "M", 9L, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.criar(new NotificacoesRequestDTO("T", "M", 1L, 77L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("notificarPorTipo resolve o tipo pelo nome")
    void notificaPorTipo() {
        prepararCriacao();
        TipoNotificacao tipo = new TipoNotificacao();
        tipo.setId(3);
        tipo.setStatus("CANDIDATURA");
        when(tipoNotificacaoRepository.findByStatusIgnoreCase("candidatura")).thenReturn(Optional.of(tipo));
        when(tipoNotificacaoRepository.findById(3)).thenReturn(Optional.of(tipo));

        service.notificarPorTipo(1L, "T", "M", "candidatura");

        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(notificacaoRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isSameAs(tipo);
    }

    @Test
    @DisplayName("marca como lida só a própria notificação")
    void marcaComoLida() {
        Notificacao propria = notificacao(1L);
        when(notificacaoRepository.findById(5L)).thenReturn(Optional.of(propria));
        when(notificacaoRepository.save(propria)).thenReturn(propria);

        service.marcarComoLida(5L, 1L);
        assertThat(propria.getLida()).isTrue();

        assertThatThrownBy(() -> service.marcarComoLida(5L, 2L)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("marca todas as não lidas do usuário")
    void marcaTodas() {
        List<Notificacao> naoLidas = List.of(notificacao(1L), notificacao(1L));
        when(notificacaoRepository.findByUsuarioIdAndLidaFalseOrderByDataEnvioDesc(1L)).thenReturn(naoLidas);

        service.marcarTodasComoLidas(1L);

        assertThat(naoLidas).allMatch(Notificacao::getLida);
        verify(notificacaoRepository).saveAll(naoLidas);
    }

    @Test
    @DisplayName("exclui a própria; de outro usuário só o admin")
    void exclui() {
        Notificacao deOutro = notificacao(2L);
        when(notificacaoRepository.findById(5L)).thenReturn(Optional.of(deOutro));

        assertThatThrownBy(() -> service.deletar(5L, 1L, false)).isInstanceOf(BusinessException.class);
        verify(notificacaoRepository, never()).delete(any());

        service.deletar(5L, 1L, true);
        verify(notificacaoRepository).delete(deOutro);
    }

    @Test
    @DisplayName("conta as não lidas")
    void contaNaoLidas() {
        when(notificacaoRepository.countByUsuarioIdAndLidaFalse(1L)).thenReturn(4L);

        assertThat(service.contarNaoLidas(1L)).isEqualTo(4L);
    }
}
