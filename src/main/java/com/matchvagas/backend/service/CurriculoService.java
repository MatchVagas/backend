package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.CurriculoResponseDTO;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Candidatura;
import com.matchvagas.backend.entity.Curriculos;
import com.matchvagas.backend.entity.Empresas;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.CurriculoMapper;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.CandidaturaRepository;
import com.matchvagas.backend.repository.CurriculoRepository;
import com.matchvagas.backend.repository.EmpresaRepository;
import com.matchvagas.backend.util.FileSignatureValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurriculoService {

    private static final long   TAMANHO_MAXIMO_BYTES = 5L * 1024 * 1024;
    private static final int    URL_EXPIRACAO_SEGUNDOS = 3600; // 1 hora

    private static final List<String> MIME_ACEITOS = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final CandidatoRepository    candidatoRepository;
    private final CurriculoRepository    curriculoRepository;
    private final CandidaturaRepository  candidaturaRepository;
    private final EmpresaRepository      empresaRepository;
    private final CurriculoMapper        curriculoMapper;
    private final SupabaseStorageService supabaseStorageService;

    @Transactional
    public CurriculoResponseDTO upload(Long usuarioId, MultipartFile arquivo) {
        validarArquivo(arquivo);

        Candidatos candidato = candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de candidato não encontrado para o usuário ID: " + usuarioId));

        // Remove arquivo anterior do Supabase se existir
        Optional<Curriculos> existente = curriculoRepository.findByCandidatoId(candidato.getId());
        existente.ifPresent(c -> supabaseStorageService.deletar(c.getCaminhoArquivo()));

        String nomeOriginal = arquivo.getOriginalFilename() != null
                ? arquivo.getOriginalFilename() : "curriculo";
        String extensao  = extrairExtensao(nomeOriginal);
        // Object path dentro do bucket: "{candidatoId}/{uuid}.{ext}"
        String objectPath = candidato.getId() + "/" + UUID.randomUUID() + "." + extensao;

        try {
            supabaseStorageService.upload(objectPath, arquivo.getBytes(), arquivo.getContentType());
        } catch (IOException e) {
            throw new BusinessException("Falha ao ler o arquivo enviado: " + e.getMessage());
        }

        Curriculos curriculo = existente.orElseGet(Curriculos::new);
        curriculo.setCandidato(candidato);
        curriculo.setNomeArquivo(nomeOriginal);
        curriculo.setCaminhoArquivo(objectPath); // object path no Supabase
        curriculo.setTamanhoArquivo(BigInteger.valueOf(arquivo.getSize()));
        curriculo.setFormatoArquivo(extensao.toUpperCase());

        Curriculos salvo = curriculoRepository.save(curriculo);
        candidato.setCurriculo(salvo);
        candidatoRepository.save(candidato);

        return toDTO(salvo, false);
    }

    @Transactional(readOnly = true)
    public CurriculoResponseDTO buscar(Long usuarioId) {
        Candidatos candidato = candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de candidato não encontrado para o usuário ID: " + usuarioId));

        Curriculos curriculo = curriculoRepository.findByCandidatoId(candidato.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum currículo cadastrado."));

        return toDTO(curriculo, true);
    }

    /**
     * Retorna um redirect 302 para a URL assinada do currículo do próprio candidato.
     */
    public ResponseEntity<Void> download(Long usuarioId) {
        Candidatos candidato = candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de candidato não encontrado para o usuário ID: " + usuarioId));

        Curriculos curriculo = curriculoRepository.findByCandidatoId(candidato.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum currículo cadastrado."));

        String url = supabaseStorageService.gerarUrlAssinada(
                curriculo.getCaminhoArquivo(), URL_EXPIRACAO_SEGUNDOS);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, url)
                .build();
    }

    /**
     * Retorna um redirect 302 para o currículo do candidato, validando que:
     *  - o usuário é uma empresa dona da vaga da candidatura
     *  - o candidato autorizou compartilhamento do currículo
     */
    public ResponseEntity<Void> downloadParaEmpresa(Long candidaturaId, Long usuarioId) {
        Empresas empresa = empresaRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new BusinessException("Nenhuma empresa vinculada a este usuário."));

        Candidatura candidatura = candidaturaRepository.findById(candidaturaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Candidatura não encontrada com ID: " + candidaturaId));

        if (!candidatura.getVaga().getEmpresas().getId().equals(empresa.getId())) {
            throw new AccessDeniedException("Esta candidatura não pertence a uma vaga da sua empresa.");
        }

        if (!candidatura.isCompartilharCurriculo()) {
            throw new AccessDeniedException(
                    "O candidato não autorizou o compartilhamento do currículo nesta candidatura.");
        }

        Curriculos curriculo = candidatura.getCandidato().getCurriculo();
        if (curriculo == null) {
            throw new ResourceNotFoundException("Este candidato não possui currículo cadastrado.");
        }

        String url = supabaseStorageService.gerarUrlAssinada(
                curriculo.getCaminhoArquivo(), URL_EXPIRACAO_SEGUNDOS);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, url)
                .build();
    }

    @Transactional
    public void deletar(Long usuarioId) {
        Candidatos candidato = candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de candidato não encontrado para o usuário ID: " + usuarioId));

        Curriculos curriculo = curriculoRepository.findByCandidatoId(candidato.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum currículo para remover."));

        supabaseStorageService.deletar(curriculo.getCaminhoArquivo());

        candidato.setCurriculo(null);
        candidatoRepository.save(candidato);
        curriculoRepository.delete(curriculo);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private CurriculoResponseDTO toDTO(Curriculos curriculo, boolean gerarUrl) {
        CurriculoResponseDTO base = curriculoMapper.toResponseDTO(curriculo);
        String urlArquivo = gerarUrl
                ? supabaseStorageService.gerarUrlAssinada(curriculo.getCaminhoArquivo(), URL_EXPIRACAO_SEGUNDOS)
                : null;
        return new CurriculoResponseDTO(
                base.id(), base.candidatoId(), base.nomeArquivo(), base.caminhoArquivo(),
                base.dataUpload(), base.tamanhoArquivo(), base.formatoArquivo(), urlArquivo);
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new BusinessException("Nenhum arquivo enviado.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new BusinessException("Arquivo excede o limite de 5 MB.");
        }
        String mime = arquivo.getContentType();
        if (mime == null || !MIME_ACEITOS.contains(mime)) {
            throw new BusinessException("Formato não aceito. Use PDF, DOC ou DOCX.");
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
            throw new BusinessException("Falha ao ler o arquivo enviado: " + e.getMessage());
        }
    }

    private String extrairExtensao(String nome) {
        int idx = nome.lastIndexOf('.');
        return idx >= 0 ? nome.substring(idx + 1).toLowerCase() : "pdf";
    }
}
