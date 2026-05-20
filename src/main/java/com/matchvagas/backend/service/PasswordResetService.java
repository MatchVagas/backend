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

import java.security.SecureRandom;
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

    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public void solicitarRedefinicao(String email) {
        usuariosRepository.findByEmail(email).ifPresent(usuario -> {
            tokenRepository.deleteExpiredAndUsed(LocalDateTime.now());

            String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setCodigo(codigo);
            resetToken.setUsuario(usuario);
            tokenRepository.save(resetToken);

            enviarEmailCodigo(usuario, codigo);
        });
    }

    @Transactional
    public String verificarCodigo(String email, String codigo) {
        PasswordResetToken resetToken = tokenRepository
                .findValidByCodigo(email, codigo, LocalDateTime.now())
                .orElseThrow(() -> new BusinessException("Código inválido ou expirado."));

        return resetToken.getToken();
    }

    @Transactional
    public void redefinirSenha(String token, String novaSenha) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Token inválido ou expirado."));

        if (resetToken.isUsed()) {
            throw new BusinessException("Este código já foi utilizado.");
        }
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Código expirado. Solicite um novo.");
        }

        Usuarios usuario = resetToken.getUsuario();
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuariosRepository.save(usuario);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Senha redefinida para o usuário {}", usuario.getEmail());
    }

    private void enviarEmailCodigo(Usuarios usuario, String codigo) {
        String corpo = """
                <html><body style="font-family:Arial,sans-serif;color:#333">
                  <h2>Redefinição de senha — MatchVagas</h2>
                  <p>Olá, <strong>%s</strong>!</p>
                  <p>Recebemos uma solicitação para redefinir a senha da sua conta.</p>
                  <p>Use o código abaixo no aplicativo. Ele é válido por <strong>1 hora</strong>.</p>
                  <div style="margin:24px 0;text-align:center">
                    <span style="font-size:36px;font-weight:bold;letter-spacing:8px;color:#2563eb">%s</span>
                  </div>
                  <p style="color:#888;font-size:13px">Se você não fez esta solicitação, ignore este e-mail — sua senha permanece a mesma.</p>
                </body></html>
                """.formatted(usuario.getNome(), codigo);

        emailService.enviarEmail(usuario.getEmail(), "Código de redefinição de senha — MatchVagas", corpo);
    }
}
