package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.CandidaturaRequest;
import com.matchvagas.backend.dto.CandidaturaResponse;
import com.matchvagas.backend.DuplicateCandidaturaException;
import com.matchvagas.backend.exception.NotFoundException;
import com.matchvagas.backend.exception.UnauthorizedException;
import com.matchvagas.backend.Candidatura;
import com.matchvagas.backend.CandidaturaStatus;
import com.matchvagas.backend.repository.CandidaturaRepository;
import com.matchvagas.backend.service.CandidaturaService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CandidaturaServiceImpl implements CandidaturaService {

    private final CandidaturaRepository repository;

    public CandidaturaServiceImpl(CandidaturaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public CandidaturaResponse candidatar(CandidaturaRequest request, String username) {
        // valida duplicata por consulta rápida
        if (repository.existsByVagaIdAndCandidatoUsername(request.getVagaId(), username)) {
            throw new DuplicateCandidaturaException("Usuário já se candidatou para essa vaga");
        }

        Candidatura candidatura = new Candidatura();
        candidatura.setVagaId(request.getVagaId());
        candidatura.setCandidatoUsername(username);
        candidatura.setCartaApresentacao(request.getCartaApresentacao());
        candidatura.setStatus(CandidaturaStatus.PENDENTE);

        try {
            candidatura = repository.save(candidatura);
        } catch (DataIntegrityViolationException ex) {
            // proteção contra condição de corrida: unique constraint no banco
            throw new DuplicateCandidaturaException("Candidatura duplicada detectada");
        }

        return toResponse(candidatura);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CandidaturaResponse> listarPorUsuario(String username) {
        return repository.findByCandidatoUsername(username)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CandidaturaResponse buscarPorId(Long id, String username) {
        // busca garantindo que o usuário seja dono da candidatura
        Candidatura candidatura = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Candidatura não encontrada"));

        if (!candidatura.getCandidatoUsername().equals(username)) {
            throw new UnauthorizedException("Acesso negado à candidatura");
        }

        return toResponse(candidatura);
    }

    @Override
    @Transactional
    public CandidaturaResponse mudarStatus(Long id, CandidaturaStatus novoStatus, String username) {
        // Exemplo: apenas usuários com papel administrativo podem mudar status.
        // Aqui assumimos que a verificação de papel foi feita antes ou username "admin" é um placeholder.
        boolean isAdmin = isAdmin(username);
        if (!isAdmin) {
            throw new UnauthorizedException("Somente administradores podem alterar o status");
        }

        Candidatura candidatura = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Candidatura não encontrada"));

        // valida transições de status simples (exemplo)
        if (candidatura.getStatus() == CandidaturaStatus.APROVADA && novoStatus == CandidaturaStatus.PENDENTE) {
            throw new InvalidOperationException("Transição de status inválida");
        }

        candidatura.setStatus(novoStatus);
        candidatura = repository.save(candidatura);

        return toResponse(candidatura);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean jaCandidatou(Long vagaId, String username) {
        return repository.existsByVagaIdAndCandidatoUsername(vagaId, username);
    }

    private CandidaturaResponse toResponse(Candidatura c) {
        CandidaturaResponse r = new CandidaturaResponse();
        r.setId(c.getId());
        r.setVagaId(c.getVagaId());
        r.setCandidatoUsername(c.getCandidatoUsername());
        r.setCartaApresentacao(c.getCartaApresentacao());
        r.setCriadoEm(c.getCriadoEm());
        r.setStatus(c.getStatus());
        return r;
    }

    // método placeholder para verificação de papel
    private boolean isAdmin(String username) {
        // implementar verificação real de roles (ex: consultar serviço de usuários ou token)
        return "admin".equalsIgnoreCase(username);
    }
}
