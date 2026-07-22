package com.matchvagas.backend.repository;

import com.matchvagas.backend.entity.Mensagem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensagemRepository extends JpaRepository<Mensagem, Long> {

    // Conversa em ordem cronológica (mais antigas primeiro), paginada.
    Page<Mensagem> findByCandidaturaIdOrderByDataEnvioAsc(Long candidaturaId, Pageable pageable);

    // Mensagens recebidas pelo usuário (enviadas pela outra parte) ainda não lidas.
    List<Mensagem> findByCandidaturaIdAndRemetenteIdNotAndLidaFalse(Long candidaturaId, Long usuarioId);

    long countByCandidaturaIdAndRemetenteIdNotAndLidaFalse(Long candidaturaId, Long usuarioId);

    // Total de não lidas em todas as conversas em que o usuário é participante
    // (candidato dono da candidatura OU gestor da empresa da vaga).
    @Query("""
            select count(m) from Mensagem m
            where m.lida = false
              and m.remetente.id <> :usuarioId
              and (m.candidatura.candidato.usuario.id = :usuarioId
                   or m.candidatura.vaga.empresas.usuario.id = :usuarioId)
            """)
    long contarNaoLidasPorParticipante(@Param("usuarioId") Long usuarioId);

    // ── Limpeza de FK antes de remover candidaturas ──────────────────────────
    @Modifying
    @Query("delete from Mensagem m where m.candidatura.id = :candidaturaId")
    void deleteByCandidaturaId(@Param("candidaturaId") Long candidaturaId);

    @Modifying
    @Query("delete from Mensagem m where m.candidatura in "
            + "(select c from Candidatura c where c.vaga.id = :vagaId)")
    void deleteByCandidaturaVagaId(@Param("vagaId") Long vagaId);
}
