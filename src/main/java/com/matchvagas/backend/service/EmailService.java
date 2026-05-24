package com.matchvagas.backend.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final Resend resend;
    private final String fromAddress;

    public EmailService(
            @Value("${resend.api-key:}") String apiKey,
            @Value("${resend.from:}") String fromAddress) {
        this.fromAddress = fromAddress;
        this.resend = apiKey.isBlank() ? null : new Resend(apiKey);
    }

    public void enviarEmail(String para, String assunto, String corpoHtml) {
        if (resend == null) {
            log.warn("Resend não configurado (RESEND_API_KEY ausente) — email não enviado para {}", para);
            return;
        }
        try {
            CreateEmailOptions options = CreateEmailOptions.builder()
                    .from(fromAddress)
                    .to(para)
                    .subject(assunto)
                    .html(corpoHtml)
                    .build();
            resend.emails().send(options);
            log.info("Email enviado para {}", para);
        } catch (ResendException e) {
            log.error("Falha ao enviar email para {}: {}", para, e.getMessage());
        }
    }
}
