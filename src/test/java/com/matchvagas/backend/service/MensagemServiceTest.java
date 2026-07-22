package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.MensagemRequestDTO;
import com.matchvagas.backend.dto.MensagemResponseDTO;
import com.matchvagas.backend.dto.PageResponseDTO;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Candidatura;
import com.matchvagas.backend.entity.Empresas;
import com.matchvagas.backend.entity.Mensagem;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.entity.Vagas;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.repository.CandidaturaRepository;
import com.matchvagas.backend.repository.MensagemRepository;
import com.matchvagas.backend.repository.UsuariosRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fase 2 — Mensagens (conversa empresa ↔ candidato)")
class MensagemServiceTest {

    @Mock MensagemRepository    mensagemRepository;
    @Mock CandidaturaRepository candidaturaRepository;
    @Mock UsuariosRepository    usuariosRepository;
    @Mock NotificacaoService    notificacaoService;
    @Mock RealtimeService       realtimeService;

    @InjectMocks MensagemService mensagemService;

    private static final Long USUARIO_CANDIDATO_ID = 1L;
    private static final Long USUARIO_EMPRESA_ID   = 20L;
    private static final Long ESTRANHO_ID          = 999L;
    private static final Long CANDIDATURA_ID       = 100L;
    private static final Pageable PAGEABLE         = PageRequest.of(0, 30);

    private Usuarios   usuarioCandidato;
    private Usuarios   usuarioEmpresa;
    private Candidatura candidatura;

    @BeforeEach
    void setUp() {
        usuarioCandidato = new Usuarios();
        usuarioCandidato.setId(USUARIO_CANDIDATO_ID);
        usuarioCandidato.setNome("Carlos Souza");

        Candidatos candidato = new Candidatos();
        candidato.setId(10L);
        candidato.setUsuario(usuarioCandidato);

        usuarioEmpresa = new Usuarios();
        usuarioEmpresa.setId(USUARIO_EMPRESA_ID);
        usuarioEmpresa.setNome("Tech Corp RH");

        Empresas empresa = new Empresas();
        empresa.setId(5L);
        empresa.setNomeFantasia("Tech Corp");
        empresa.setUsuario(usuarioEmpresa);

        Vagas vaga = new Vagas();
        vaga.setId(5L);
        vaga.setTitulo("Dev Java Pleno");
        vaga.setEmpresas(empresa);

        candidatura = new Candidatura();
        candidatura.setId(CANDIDATURA_ID);
        candidatura.setCandidato(candidato);
        candidatura.setVaga(vaga);
    }

    private Mensagem mensagemDe(Usuarios remetente, String conteudo, boolean lida) {
        Mensagem m = new Mensagem();
        m.setId(500L);
        m.setCandidatura(candidatura);
        m.setRemetente(remetente);
        m.setConteudo(conteudo);
        m.setDataEnvio(LocalDateTime.now());
        m.setLida(lida);
        return m;
    }

    @Nested
    @DisplayName("Enviar mensagem")
    class Enviar {

        @Test
        @DisplayName("Candidato envia → persiste e notifica a empresa (com e-mail, destinatário em dia)")
        void candidatoEnviaNotificaEmpresa() {
            MensagemRequestDTO dto = new MensagemRequestDTO(CANDIDATURA_ID, "Tenho interesse na vaga!");

            when(candidaturaRepository.findById(CANDIDATURA_ID)).thenReturn(Optional.of(candidatura));
            when(usuariosRepository.findById(USUARIO_CANDIDATO_ID)).thenReturn(Optional.of(usuarioCandidato));
            when(mensagemRepository.countByCandidaturaIdAndRemetenteIdNotAndLidaFalse(
                    CANDIDATURA_ID, USUARIO_EMPRESA_ID)).thenReturn(0L);
            when(mensagemRepository.save(any(Mensagem.class))).thenAnswer(inv -> {
                Mensagem m = inv.getArgument(0);
                m.setId(500L);
                return m;
            });

            MensagemResponseDTO result = mensagemService.enviar(USUARIO_CANDIDATO_ID, dto);

            assertThat(result.remetentePapel()).isEqualTo("CANDIDATO");
            assertThat(result.remetenteId()).isEqualTo(USUARIO_CANDIDATO_ID);
            assertThat(result.conteudo()).isEqualTo("Tenho interesse na vaga!");
            assertThat(result.lida()).isFalse();
            // Destinatário é o gestor da empresa (id 20); estava em dia → e-mail habilitado
            verify(notificacaoService).notificarPorTipo(
                    eq(USUARIO_EMPRESA_ID), eq("Nova mensagem"), any(), eq("Mensagem"), eq(true));
            // Push SSE ao destinatário para atualizar a conversa aberta
            verify(realtimeService).enviarPara(eq(USUARIO_EMPRESA_ID), eq("mensagem"), any());
        }

        @Test
        @DisplayName("Empresa envia → persiste e notifica o candidato")
        void empresaEnviaNotificaCandidato() {
            MensagemRequestDTO dto = new MensagemRequestDTO(CANDIDATURA_ID, "Vamos agendar uma entrevista?");

            when(candidaturaRepository.findById(CANDIDATURA_ID)).thenReturn(Optional.of(candidatura));
            when(usuariosRepository.findById(USUARIO_EMPRESA_ID)).thenReturn(Optional.of(usuarioEmpresa));
            when(mensagemRepository.countByCandidaturaIdAndRemetenteIdNotAndLidaFalse(
                    CANDIDATURA_ID, USUARIO_CANDIDATO_ID)).thenReturn(0L);
            when(mensagemRepository.save(any(Mensagem.class))).thenAnswer(inv -> inv.getArgument(0));

            MensagemResponseDTO result = mensagemService.enviar(USUARIO_EMPRESA_ID, dto);

            assertThat(result.remetentePapel()).isEqualTo("EMPRESA");
            verify(notificacaoService).notificarPorTipo(
                    eq(USUARIO_CANDIDATO_ID), eq("Nova mensagem"), any(), eq("Mensagem"), eq(true));
        }

        @Test
        @DisplayName("Anti-spam: destinatário com não lidas pendentes recebe só in-app, sem e-mail")
        void naoReenviaEmailQuandoDestinatarioTemNaoLidas() {
            MensagemRequestDTO dto = new MensagemRequestDTO(CANDIDATURA_ID, "Continuando o assunto...");

            when(candidaturaRepository.findById(CANDIDATURA_ID)).thenReturn(Optional.of(candidatura));
            when(usuariosRepository.findById(USUARIO_CANDIDATO_ID)).thenReturn(Optional.of(usuarioCandidato));
            // Empresa (destinatário) ainda tem 2 mensagens não lidas nesta conversa
            when(mensagemRepository.countByCandidaturaIdAndRemetenteIdNotAndLidaFalse(
                    CANDIDATURA_ID, USUARIO_EMPRESA_ID)).thenReturn(2L);
            when(mensagemRepository.save(any(Mensagem.class))).thenAnswer(inv -> inv.getArgument(0));

            mensagemService.enviar(USUARIO_CANDIDATO_ID, dto);

            // in-app criado, mas e-mail suprimido (enviarEmail = false)
            verify(notificacaoService).notificarPorTipo(
                    eq(USUARIO_EMPRESA_ID), eq("Nova mensagem"), any(), eq("Mensagem"), eq(false));
        }

        @Test
        @DisplayName("Usuário que não participa da candidatura é barrado")
        void estranhoNaoPodeEnviar() {
            MensagemRequestDTO dto = new MensagemRequestDTO(CANDIDATURA_ID, "Deixa eu me intrometer");

            when(candidaturaRepository.findById(CANDIDATURA_ID)).thenReturn(Optional.of(candidatura));

            assertThatThrownBy(() -> mensagemService.enviar(ESTRANHO_ID, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("não participa");

            verify(mensagemRepository, never()).save(any());
            verifyNoInteractions(notificacaoService);
        }

        @Test
        @DisplayName("Candidatura inexistente → ResourceNotFoundException")
        void candidaturaInexistente() {
            MensagemRequestDTO dto = new MensagemRequestDTO(CANDIDATURA_ID, "Olá?");

            when(candidaturaRepository.findById(CANDIDATURA_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> mensagemService.enviar(USUARIO_CANDIDATO_ID, dto))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(mensagemRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Listar conversa")
    class Listar {

        @Test
        @DisplayName("Participante vê a conversa paginada com o papel de cada remetente")
        void participanteVeConversa() {
            when(candidaturaRepository.findById(CANDIDATURA_ID)).thenReturn(Optional.of(candidatura));
            when(mensagemRepository.findByCandidaturaIdOrderByDataEnvioAsc(CANDIDATURA_ID, PAGEABLE))
                    .thenReturn(new PageImpl<>(
                            List.of(mensagemDe(usuarioCandidato, "Oi", true),
                                    mensagemDe(usuarioEmpresa, "Olá, tudo bem?", false)),
                            PAGEABLE, 2));

            PageResponseDTO<MensagemResponseDTO> result =
                    mensagemService.listar(USUARIO_EMPRESA_ID, CANDIDATURA_ID, PAGEABLE);

            assertThat(result.content()).hasSize(2);
            assertThat(result.content().get(0).remetentePapel()).isEqualTo("CANDIDATO");
            assertThat(result.content().get(1).remetentePapel()).isEqualTo("EMPRESA");
        }

        @Test
        @DisplayName("Não participante não lista a conversa")
        void estranhoNaoLista() {
            when(candidaturaRepository.findById(CANDIDATURA_ID)).thenReturn(Optional.of(candidatura));

            assertThatThrownBy(() -> mensagemService.listar(ESTRANHO_ID, CANDIDATURA_ID, PAGEABLE))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("não participa");
        }
    }

    @Nested
    @DisplayName("Leitura")
    class Leitura {

        @Test
        @DisplayName("Marca como lidas apenas as mensagens recebidas da outra parte")
        void marcaRecebidasComoLidas() {
            Mensagem m1 = mensagemDe(usuarioEmpresa, "Mensagem 1", false);
            Mensagem m2 = mensagemDe(usuarioEmpresa, "Mensagem 2", false);

            when(candidaturaRepository.findById(CANDIDATURA_ID)).thenReturn(Optional.of(candidatura));
            when(mensagemRepository.findByCandidaturaIdAndRemetenteIdNotAndLidaFalse(
                    CANDIDATURA_ID, USUARIO_CANDIDATO_ID)).thenReturn(List.of(m1, m2));

            int marcadas = mensagemService.marcarComoLidas(USUARIO_CANDIDATO_ID, CANDIDATURA_ID);

            assertThat(marcadas).isEqualTo(2);
            assertThat(m1.isLida()).isTrue();
            assertThat(m2.isLida()).isTrue();
            verify(mensagemRepository).saveAll(List.of(m1, m2));
        }

        @Test
        @DisplayName("Conta não lidas da conversa delegando ao repositório")
        void contaNaoLidasDaConversa() {
            when(candidaturaRepository.findById(CANDIDATURA_ID)).thenReturn(Optional.of(candidatura));
            when(mensagemRepository.countByCandidaturaIdAndRemetenteIdNotAndLidaFalse(
                    CANDIDATURA_ID, USUARIO_CANDIDATO_ID)).thenReturn(3L);

            assertThat(mensagemService.contarNaoLidas(USUARIO_CANDIDATO_ID, CANDIDATURA_ID)).isEqualTo(3L);
        }

        @Test
        @DisplayName("Conta não lidas globais do usuário")
        void contaNaoLidasGlobais() {
            when(mensagemRepository.contarNaoLidasPorParticipante(USUARIO_CANDIDATO_ID)).thenReturn(7L);

            assertThat(mensagemService.contarNaoLidasTotais(USUARIO_CANDIDATO_ID)).isEqualTo(7L);
        }
    }
}
