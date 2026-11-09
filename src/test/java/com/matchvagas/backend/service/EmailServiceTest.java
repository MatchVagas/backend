package com.matchvagas.backend.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Envio de e-mail (Resend / Gmail)")
class EmailServiceTest {

    @SuppressWarnings("unchecked")
    private ObjectProvider<JavaMailSender> provider(JavaMailSender sender) {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        return provider;
    }

    @Test
    @DisplayName("Gmail configurado envia mensagem HTML com remetente e destinatário")
    void enviaPorGmail() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage mensagem = new MimeMessage((Session) null);
        when(sender.createMimeMessage()).thenReturn(mensagem);
        var service = new EmailService(true, "", "", "app@gmail.com", provider(sender));

        service.enviarEmail("ana@x.com", "Assunto", "<b>Olá</b>");

        verify(sender).send(mensagem);
        assertThat(mensagem.getSubject()).isEqualTo("Assunto");
        assertThat(mensagem.getAllRecipients()[0].toString()).isEqualTo("ana@x.com");
        assertThat(mensagem.getFrom()[0].toString()).isEqualTo("app@gmail.com");
    }

    @Test
    @DisplayName("Gmail sem remetente configurado não envia nem falha")
    void gmailNaoConfigurado() {
        JavaMailSender sender = mock(JavaMailSender.class);
        var service = new EmailService(true, "", "", "", provider(sender));

        service.enviarEmail("ana@x.com", "A", "B");

        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("falha do SMTP é registrada e não propaga")
    void falhaSmtp() {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new MailSendException("fora")).when(sender).send(any(MimeMessage.class));
        var service = new EmailService(true, "", "", "app@gmail.com", provider(sender));

        assertThatCode(() -> service.enviarEmail("ana@x.com", "A", "B")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("escapa HTML do texto do usuário, mantendo acentos; nulo vira vazio")
    void escapaHtml() {
        assertThat(EmailService.html("<script>alert('x')</script> João & Cia"))
                .isEqualTo("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; João &amp; Cia");
        assertThat(EmailService.html(null)).isEmpty();
    }

    @Test
    @DisplayName("Resend sem API key não envia nem falha")
    void resendNaoConfigurado() {
        var service = new EmailService(false, "", "", "", provider(null));

        assertThatCode(() -> service.enviarEmail("ana@x.com", "A", "B")).doesNotThrowAnyException();
    }
}
