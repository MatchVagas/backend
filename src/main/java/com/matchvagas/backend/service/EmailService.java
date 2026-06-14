package com.matchvagas.backend.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final boolean usarGmail;

    // Provedor padrão: Resend (API HTTP)
    private final Resend resend;
    private final String resendFrom;

    // Backup: Gmail / SMTP (via spring-boot-starter-mail)
    private final JavaMailSender mailSender;
    private final String gmailFrom;

    public EmailService(
            @Value("${mail.use-gmail:false}") boolean usarGmail,
            @Value("${resend.api-key:}") String apiKey,
            @Value("${resend.from:}") String resendFrom,
            @Value("${spring.mail.username:}") String gmailFrom,
            org.springframework.beans.factory.ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.usarGmail = usarGmail;
        this.resendFrom = resendFrom;
        this.resend = apiKey.isBlank() ? null : new Resend(apiKey);
        this.gmailFrom = gmailFrom;
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    public void enviarEmail(String para, String assunto, String corpoHtml) {
        if (usarGmail) {
            enviarPorGmail(para, assunto, corpoHtml);
        } else {
            enviarPorResend(para, assunto, corpoHtml);
        }
    }

    private void enviarPorResend(String para, String assunto, String corpoHtml) {
        if (resend == null) {
            log.warn("Resend não configurado (RESEND_API_KEY ausente) — email não enviado para {}", para);
            return;
        }
        try {
            CreateEmailOptions options = CreateEmailOptions.builder()
                    .from(resendFrom)
                    .to(para)
                    .subject(assunto)
                    .html(corpoHtml)
                    .build();
            resend.emails().send(options);
            log.info("Email enviado via Resend para {}", para);
        } catch (ResendException e) {
            log.error("Falha ao enviar email via Resend para {}: {}", para, e.getMessage());
        }
    }

    private void enviarPorGmail(String para, String assunto, String corpoHtml) {
        if (mailSender == null || gmailFrom.isBlank()) {
            log.warn("Gmail/SMTP não configurado (MAIL_HOST/MAIL_USERNAME ausentes) — email não enviado para {}", para);
            return;
        }
        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, "UTF-8");
            helper.setFrom(gmailFrom);
            helper.setTo(para);
            helper.setSubject(assunto);
            helper.setText(corpoHtml, true);
            mailSender.send(mensagem);
            log.info("Email enviado via Gmail/SMTP para {}", para);
        } catch (MessagingException | org.springframework.mail.MailException e) {
            log.error("Falha ao enviar email via Gmail/SMTP para {}: {}", para, e.getMessage());
        }
    }
}
