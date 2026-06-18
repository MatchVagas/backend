package com.matchvagas.backend.service;

import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.CurriculoRepository;
import com.matchvagas.backend.repository.PasswordResetTokenRepository;
import com.matchvagas.backend.repository.UsuariosRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Política de retenção de dados (LGPD Art. 15/16). Job mensal que:
 *  - pseudoanonimiza candidatos inativos há mais de N anos (remove dados
 *    identificadores: nome, e-mail, endereço, telefone, foto e perfil);
 *  - remove currículos de candidatos inativos há mais de M anos;
 *  - limpa tokens de redefinição de senha expirados/utilizados.
 *
 * <p>Periodicidade e prazos são configuráveis; o job pode ser desligado via
 * {@code lgpd.retencao.enabled=false}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetencaoDadosService {

    private final UsuariosRepository usuariosRepository;
    private final CandidatoRepository candidatoRepository;
    private final CurriculoRepository curriculoRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final CurriculoService curriculoService;
    private final FotoPerfilService fotoPerfilService;

    @Value("${lgpd.retencao.enabled:true}")
    private boolean habilitado;

    @Value("${lgpd.retencao.inatividade-anos:2}")
    private int inatividadeAnos;

    @Value("${lgpd.retencao.curriculo-inatividade-anos:1}")
    private int curriculoInatividadeAnos;

    // Todo dia 1 de cada mês às 3h
    @Scheduled(cron = "${lgpd.retencao.cron:0 0 3 1 * ?}")
    @Transactional
    public void executar() {
        if (!habilitado) {
            return;
        }
        LocalDateTime agora = LocalDateTime.now();

        passwordResetTokenRepository.deleteExpiredAndUsed(agora);

        removerCurriculosInativos(agora.minusYears(curriculoInatividadeAnos));
        pseudoanonimizarInativos(agora.minusYears(inatividadeAnos));
    }

    private void removerCurriculosInativos(LocalDateTime limite) {
        List<Usuarios> inativos = usuariosRepository
                .findByTipoUsuarioAndAtivoAndDataUltimoAcessoBefore(
                        Usuarios.TipoUsuario.CANDIDATO, true, limite);
        int removidos = 0;
        for (Usuarios usuario : inativos) {
            Candidatos candidato = candidatoRepository.findByUsuarioId(usuario.getId()).orElse(null);
            if (candidato != null
                    && curriculoRepository.findByCandidatoId(candidato.getId()).isPresent()) {
                try {
                    curriculoService.deletar(usuario.getId());
                    removidos++;
                } catch (Exception e) {
                    log.warn("Falha ao remover currículo do usuário {}: {}", usuario.getId(), e.getMessage());
                }
            }
        }
        if (removidos > 0) {
            log.info("Retenção LGPD: {} currículo(s) de candidatos inativos removido(s).", removidos);
        }
    }

    private void pseudoanonimizarInativos(LocalDateTime limite) {
        List<Usuarios> inativos = usuariosRepository
                .findByTipoUsuarioAndAtivoAndDataUltimoAcessoBefore(
                        Usuarios.TipoUsuario.CANDIDATO, true, limite);
        int anonimizados = 0;
        for (Usuarios usuario : inativos) {
            pseudoanonimizar(usuario);
            anonimizados++;
        }
        if (anonimizados > 0) {
            log.info("Retenção LGPD: {} candidato(s) inativo(s) pseudoanonimizado(s).", anonimizados);
        }
    }

    private void pseudoanonimizar(Usuarios usuario) {
        Candidatos candidato = candidatoRepository.findByUsuarioId(usuario.getId()).orElse(null);
        if (candidato != null) {
            if (candidato.getFotoPerfilUrl() != null) {
                try {
                    fotoPerfilService.deletarCandidato(usuario.getId());
                } catch (Exception e) {
                    log.warn("Falha ao remover foto do usuário {}: {}", usuario.getId(), e.getMessage());
                }
            }
            // LGPD-07 — o endereço só faz sentido enquanto há relação ativa
            candidato.setEndereco(null);
            candidato.setObjetivoProfissional(null);
            candidato.setDisponibilidade(null);
            candidato.setPretensaoSalarial(null);
            candidato.setGenero(null);
            if (candidato.getHabilidades() != null) {
                candidato.getHabilidades().clear();
            }
            candidatoRepository.save(candidato);
        }

        // Remove dados identificadores diretos do usuário e desativa a conta.
        // O CPF permanece cifrado em repouso (LGPD-04), protegido sem a chave.
        usuario.setNome("(conta removida)");
        usuario.setEmail("removido-" + usuario.getId() + "@anonimizado.local");
        usuario.setAtivo(false);
        if (usuario.getTelefones() != null) {
            usuario.getTelefones().clear();
        }
        usuariosRepository.save(usuario);
    }
}
