package com.matchvagas.backend.service;

import com.matchvagas.backend.entity.PasswordResetToken;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.repository.PasswordResetTokenRepository;
import com.matchvagas.backend.repository.UsuariosRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UsuariosRepository usuariosRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void solicitarRedefinicao(String email) {
        // Retorna sempre a mesma mensagem para não expor quais e-mails existem
        usuariosRepository.findByEmail(email).ifPresent(usuario -> {
            tokenRepository.deleteExpiredAndUsed(LocalDateTime.now());

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setUsuario(usuario);
            tokenRepository.save(resetToken);

            enviarEmailRedefinicao(usuario, resetToken.getToken());
        });
    }

    @Transactional
    public void redefinirSenha(String token, String novaSenha) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Token inválido ou expirado."));

        if (resetToken.isUsed()) {
            throw new BusinessException("Este link já foi utilizado.");
        }
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Token expirado. Solicite um novo link de redefinição.");
        }

        Usuarios usuario = resetToken.getUsuario();
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuariosRepository.save(usuario);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Senha redefinida para o usuário {}", usuario.getEmail());
    }

    private void enviarEmailRedefinicao(Usuarios usuario, String token) {
        String link = "http://localhost:3000/redefinir-senha?token=" + token;
        String corpo = """
                <html><body style="font-family:Arial,sans-serif;color:#333">
                  <h2>Redefinição de senha — MatchVagas</h2>
                  <p>Olá, <strong>%s</strong>!</p>
                  <p>Recebemos uma solicitação para redefinir a senha da sua conta.</p>
                  <p>Clique no botão abaixo para criar uma nova senha. O link é válido por <strong>1 hora</strong>.</p>
                  <p style="margin:24px 0">
                    <a href="%s"
                       style="background:#2563eb;color:#fff;padding:12px 24px;border-radius:6px;text-decoration:none;font-weight:bold">
                      Redefinir minha senha
                    </a>
                  </p>
                  <p style="color:#888;font-size:13px">Se você não fez esta solicitação, ignore este e-mail — sua senha permanece a mesma.</p>
                </body></html>
                """.formatted(usuario.getNome(), link);

        emailService.enviarEmail(usuario.getEmail(), "Redefinição de senha — MatchVagas", corpo);
    }
}
