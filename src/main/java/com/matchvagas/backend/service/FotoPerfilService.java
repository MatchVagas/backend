package com.matchvagas.backend.service;

import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Empresas;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.EmpresaRepository;
import com.matchvagas.backend.util.FileSignatureValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FotoPerfilService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final int  URL_EXPIRACAO_SEGUNDOS = 3600; // 1 hora
    private static final List<String> MIME_ACEITOS = List.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final CandidatoRepository    candidatoRepository;
    private final EmpresaRepository      empresaRepository;
    private final SupabaseStorageService supabaseStorageService;

    @Transactional
    public String uploadCandidato(Long usuarioId, MultipartFile arquivo) {
        validar(arquivo);

        Candidatos candidato = candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de candidato não encontrado para o usuário ID: " + usuarioId));

        if (candidato.getFotoPerfilUrl() != null) {
            try {
                supabaseStorageService.deletarImagem(extrairObjectPath(candidato.getFotoPerfilUrl()));
            } catch (Exception ignored) {}
        }

        String objectPath = buildPath("candidatos", candidato.getId(), arquivo);
        enviar(arquivo, objectPath);

        // LGPD-08 — armazena o object path; o acesso é via URL assinada (bucket privado)
        candidato.setFotoPerfilUrl(objectPath);
        candidatoRepository.save(candidato);

        return supabaseStorageService.gerarUrlAssinadaImagem(objectPath, URL_EXPIRACAO_SEGUNDOS);
    }

    @Transactional
    public void deletarCandidato(Long usuarioId) {
        Candidatos candidato = candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de candidato não encontrado para o usuário ID: " + usuarioId));

        if (candidato.getFotoPerfilUrl() == null) {
            throw new BusinessException("Nenhuma foto de perfil para remover.");
        }

        String objectPath = extrairObjectPath(candidato.getFotoPerfilUrl());
        supabaseStorageService.deletarImagem(objectPath);

        candidato.setFotoPerfilUrl(null);
        candidatoRepository.save(candidato);
    }

    @Transactional
    public String uploadEmpresa(Long usuarioId, MultipartFile arquivo) {
        validar(arquivo);

        Empresas empresa = empresaRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhuma empresa vinculada ao usuário ID: " + usuarioId));

        if (empresa.getLogoUrl() != null) {
            try {
                supabaseStorageService.deletarImagem(extrairObjectPath(empresa.getLogoUrl()));
            } catch (Exception ignored) {}
        }

        String objectPath = buildPath("empresas", empresa.getId(), arquivo);
        enviar(arquivo, objectPath);

        // LGPD-08 — armazena o object path; o acesso é via URL assinada (bucket privado)
        empresa.setLogoUrl(objectPath);
        empresaRepository.save(empresa);

        return supabaseStorageService.gerarUrlAssinadaImagem(objectPath, URL_EXPIRACAO_SEGUNDOS);
    }

    @Transactional
    public void deletarEmpresa(Long usuarioId) {
        Empresas empresa = empresaRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhuma empresa vinculada ao usuário ID: " + usuarioId));

        if (empresa.getLogoUrl() == null) {
            throw new BusinessException("Nenhuma logo para remover.");
        }

        String objectPath = extrairObjectPath(empresa.getLogoUrl());
        supabaseStorageService.deletarImagem(objectPath);

        empresa.setLogoUrl(null);
        empresaRepository.save(empresa);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void validar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new BusinessException("Nenhuma imagem enviada.");
        }
        if (arquivo.getSize() > MAX_BYTES) {
            throw new BusinessException("Imagem excede o limite de 5 MB.");
        }
        String mime = arquivo.getContentType();
        if (mime == null || !MIME_ACEITOS.contains(mime)) {
            throw new BusinessException("Formato não aceito. Use JPG, PNG ou WebP.");
        }
        // SEC-05 — valida o conteúdo real (magic bytes), não só o Content-Type
        if (!FileSignatureValidator.matches(lerCabecalho(arquivo), mime)) {
            throw new BusinessException("O conteúdo do arquivo não corresponde ao formato declarado.");
        }
    }

    private byte[] lerCabecalho(MultipartFile arquivo) {
        try (var is = arquivo.getInputStream()) {
            return is.readNBytes(12);
        } catch (IOException e) {
            throw new BusinessException("Falha ao ler a imagem enviada: " + e.getMessage());
        }
    }

    private String buildPath(String prefixo, Long entidadeId, MultipartFile arquivo) {
        String nomeOriginal = arquivo.getOriginalFilename() != null
                ? arquivo.getOriginalFilename() : "foto";
        String extensao = extrairExtensao(nomeOriginal);
        return prefixo + "/" + entidadeId + "/" + UUID.randomUUID() + "." + extensao;
    }

    private void enviar(MultipartFile arquivo, String objectPath) {
        try {
            supabaseStorageService.uploadImagem(
                    objectPath, arquivo.getBytes(), arquivo.getContentType());
        } catch (IOException e) {
            throw new BusinessException("Falha ao ler a imagem enviada: " + e.getMessage());
        }
    }

    private String extrairExtensao(String nome) {
        int idx = nome.lastIndexOf('.');
        return idx >= 0 ? nome.substring(idx + 1).toLowerCase() : "jpg";
    }

    private String extrairObjectPath(String publicUrl) {
        // URL pública: {supabaseUrl}/storage/v1/object/public/{bucket}/{path}
        // Queremos apenas o {path} após o nome do bucket
        String marker = "/object/public/";
        int idx = publicUrl.indexOf(marker);
        if (idx < 0) return publicUrl;
        String afterMarker = publicUrl.substring(idx + marker.length());
        // afterMarker = "{bucket}/{path}" → pega tudo após a primeira "/"
        int slashIdx = afterMarker.indexOf('/');
        return slashIdx >= 0 ? afterMarker.substring(slashIdx + 1) : afterMarker;
    }
}
