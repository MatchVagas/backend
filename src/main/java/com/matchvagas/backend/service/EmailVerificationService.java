package com.matchvagas.backend.service;

import com.matchvagas.backend.entity.EmailVerificationToken;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.repository.EmailVerificationTokenRepository;
import com.matchvagas.backend.repository.UsuariosRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Verificação de e-mail no cadastro público (Fase 1). Emite um token de uso
 * único (24h), envia o link por e-mail e, na confirmação, marca
 * {@code emailVerificado = true} — o que o {@link AuthService#login} exige.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final UsuariosRepository usuariosRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * Gera um novo token para o usuário (invalidando os anteriores) e envia o
     * e-mail de confirmação.
     */
    @Transactional
    public void enviarVerificacao(Usuarios usuario) {
        tokenRepository.deleteByUsuarioId(usuario.getId());

        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUsuario(usuario);
        tokenRepository.save(token);

        enviarEmail(usuario, token.getToken());
    }

    /**
     * Confirma o e-mail a partir do token. Idempotente para tokens já usados
     * cujo usuário já está verificado (retorna sem erro).
     */
    @Transactional
    public void confirmar(String tokenValor) {
        EmailVerificationToken token = tokenRepository.findByToken(tokenValor)
                .orElseThrow(() -> new BusinessException("Token de verificação inválido."));

        Usuarios usuario = token.getUsuario();

        if (token.isUsed()) {
            if (Boolean.TRUE.equals(usuario.getEmailVerificado())) {
                return; // já confirmado — nada a fazer
            }
            throw new BusinessException("Este link de verificação já foi utilizado.");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Link de verificação expirado. Solicite um novo.");
        }

        usuario.setEmailVerificado(true);
        usuariosRepository.save(usuario);

        token.setUsed(true);
        tokenRepository.save(token);

        log.info("E-mail verificado para o usuário {}", usuario.getEmail());
    }

    /**
     * Reenvia o e-mail de verificação. Silencioso por design: não revela se o
     * e-mail existe nem se já está verificado (evita enumeração de contas).
     */
    @Transactional
    public void reenviar(String email) {
        usuariosRepository.findByEmail(email).ifPresent(usuario -> {
            if (!Boolean.TRUE.equals(usuario.getEmailVerificado())) {
                enviarVerificacao(usuario);
            }
        });
    }

    private void enviarEmail(Usuarios usuario, String token) {
        String link = frontendUrl + "/confirmar-email?token=" + token;
        String corpo = """
                <html><body style="font-family:Arial,sans-serif;color:#333">
                  <h2>Confirme seu e-mail — MatchVagas</h2>
                  <p>Olá, <strong>%s</strong>!</p>
                  <p>Falta pouco para ativar sua conta. Confirme seu e-mail clicando no botão abaixo
                     (o link é válido por <strong>24 horas</strong>).</p>
                  <div style="margin:24px 0;text-align:center">
                    <a href="%s" style="background:#2563eb;color:#fff;padding:12px 24px;
                       text-decoration:none;border-radius:6px;font-weight:bold">Confirmar e-mail</a>
                  </div>
                  <p style="color:#888;font-size:13px">Se você não criou esta conta, ignore este e-mail.</p>
                </body></html>
                """.formatted(usuario.getNome(), link);

        emailService.enviarEmail(usuario.getEmail(), "Confirme seu e-mail — MatchVagas", corpo);
    }
}
