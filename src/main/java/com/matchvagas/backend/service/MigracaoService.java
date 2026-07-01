package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.MigracaoResultadoDTO;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Empresas;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.EmpresaRepository;
import com.matchvagas.backend.util.CpfCrypto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Migrações operacionais pontuais executadas após o deploy das correções de
 * segurança/LGPD. São idempotentes (podem rodar mais de uma vez sem efeito
 * colateral) e restritas a ADMIN (ver mapeamento em {@code /api/admin/migracao}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MigracaoService {

    // URL pública do Supabase: {url}/storage/v1/object/public/{bucket}/{path}
    private static final String MARCADOR_URL_PUBLICA = "/object/public/";

    private final CandidatoRepository candidatoRepository;
    private final EmpresaRepository   empresaRepository;

    /**
     * Backfill de CPF (LGPD-04): cifra em repouso os CPFs legados (texto puro) e
     * popula {@code cpf_hash} para os registros que ainda estão sem hash.
     *
     * <p>O marcador de "pendente" é {@code cpf_hash == null}: o hook
     * {@code @PreUpdate} da entidade sempre popula o hash, então qualquer registro
     * sem hash é anterior à migração. Ao definir o hash o registro fica "sujo",
     * o Hibernate emite um UPDATE e o {@code CpfCryptoConverter} cifra a coluna
     * {@code cpf} automaticamente.
     */
    @Transactional
    public MigracaoResultadoDTO backfillCpf() {
        List<Candidatos> candidatos = candidatoRepository.findAll();
        int atualizados = 0, jaProcessados = 0, semValor = 0, erros = 0;
        List<String> mensagens = new ArrayList<>();

        for (Candidatos c : candidatos) {
            try {
                String cpf = c.getCpf(); // já decifrado pelo converter (texto puro)
                if (cpf == null || cpf.isBlank()) {
                    semValor++;
                    continue;
                }
                if (c.getCpfHash() != null && !c.getCpfHash().isBlank()) {
                    jaProcessados++;
                    continue;
                }
                // Marca o registro como sujo → UPDATE → converter cifra a coluna cpf
                // e @PreUpdate reconfirma o hash a partir do mesmo valor em texto puro.
                c.setCpfHash(CpfCrypto.hash(cpf));
                candidatoRepository.save(c);
                atualizados++;
            } catch (Exception e) {
                erros++;
                mensagens.add("Candidato id=" + c.getId() + ": " + e.getMessage());
                log.warn("Falha no backfill de CPF do candidato id={}", c.getId(), e);
            }
        }

        log.info("Backfill de CPF concluído: {} atualizados, {} já processados, {} sem CPF, {} erros",
                atualizados, jaProcessados, semValor, erros);
        return new MigracaoResultadoDTO("backfill-cpf", candidatos.size(),
                atualizados, jaProcessados, semValor, erros, mensagens);
    }

    /**
     * Normalização de imagens (LGPD-08): converte URLs públicas completas antigas
     * de foto de perfil / logo em <b>object path</b> relativo ao bucket, que é o
     * formato esperado pelo código novo (bucket privado + URL assinada).
     *
     * <p>Valores que já são object path (sem o marcador {@code /object/public/})
     * são ignorados, tornando a migração idempotente.
     *
     * <p>Obs.: apenas reescreve a referência no banco; não move o arquivo entre
     * buckets. Torne o bucket privado no painel do Supabase antes/depois conforme
     * o plano de deploy.
     */
    @Transactional
    public MigracaoResultadoDTO normalizarUrlsImagens() {
        int atualizados = 0, jaProcessados = 0, semValor = 0, erros = 0;
        List<String> mensagens = new ArrayList<>();

        List<Candidatos> candidatos = candidatoRepository.findAll();
        for (Candidatos c : candidatos) {
            String url = c.getFotoPerfilUrl();
            if (url == null || url.isBlank()) { semValor++; continue; }
            if (!contemUrlPublica(url)) { jaProcessados++; continue; }
            try {
                c.setFotoPerfilUrl(extrairObjectPath(url));
                candidatoRepository.save(c);
                atualizados++;
            } catch (Exception e) {
                erros++;
                mensagens.add("Candidato id=" + c.getId() + ": " + e.getMessage());
            }
        }

        List<Empresas> empresas = empresaRepository.findAll();
        for (Empresas emp : empresas) {
            String url = emp.getLogoUrl();
            if (url == null || url.isBlank()) { semValor++; continue; }
            if (!contemUrlPublica(url)) { jaProcessados++; continue; }
            try {
                emp.setLogoUrl(extrairObjectPath(url));
                empresaRepository.save(emp);
                atualizados++;
            } catch (Exception e) {
                erros++;
                mensagens.add("Empresa id=" + emp.getId() + ": " + e.getMessage());
            }
        }

        int total = candidatos.size() + empresas.size();
        log.info("Normalização de URLs de imagem concluída: {} atualizados, {} já processados, {} sem imagem, {} erros",
                atualizados, jaProcessados, semValor, erros);
        return new MigracaoResultadoDTO("normalizar-urls-imagens", total,
                atualizados, jaProcessados, semValor, erros, mensagens);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private boolean contemUrlPublica(String valor) {
        return valor.contains(MARCADOR_URL_PUBLICA);
    }

    /**
     * Extrai o object path de uma URL pública do Supabase.
     * Ex.: {@code .../object/public/imagens-perfil/candidatos/42/x.png}
     *      → {@code candidatos/42/x.png}
     */
    private String extrairObjectPath(String publicUrl) {
        int idx = publicUrl.indexOf(MARCADOR_URL_PUBLICA);
        if (idx < 0) {
            return publicUrl;
        }
        String aposMarcador = publicUrl.substring(idx + MARCADOR_URL_PUBLICA.length());
        // aposMarcador = "{bucket}/{path}" → remove o nome do bucket (primeiro segmento)
        int barra = aposMarcador.indexOf('/');
        return barra >= 0 ? aposMarcador.substring(barra + 1) : aposMarcador;
    }
}
