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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurriculoService {

    private static final long TAMANHO_MAXIMO_BYTES = 5L * 1024 * 1024; // 5 MB

    private static final List<String> MIME_ACEITOS = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final CandidatoRepository  candidatoRepository;
    private final CurriculoRepository  curriculoRepository;
    private final CandidaturaRepository candidaturaRepository;
    private final EmpresaRepository    empresaRepository;
    private final CurriculoMapper      curriculoMapper;

    @Transactional
    public CurriculoResponseDTO upload(Long usuarioId, MultipartFile arquivo) {
        validarArquivo(arquivo);

        Candidatos candidato = candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de candidato não encontrado para o usuário ID: " + usuarioId));

        // Remove currículo anterior do disco se existir
        Optional<Curriculos> existente = curriculoRepository.findByCandidatoId(candidato.getId());
        existente.ifPresent(c -> deletarArquivoDoDisco(c.getCaminhoArquivo()));

        String nomeOriginal = arquivo.getOriginalFilename() != null
                ? arquivo.getOriginalFilename() : "curriculo";
        String extensao = extrairExtensao(nomeOriginal);
        String nomeArquivo = UUID.randomUUID() + "." + extensao;

        Path diretorio = Paths.get(uploadDir, "curriculos", String.valueOf(candidato.getId()));
        Path destino = diretorio.resolve(nomeArquivo);

        try {
            Files.createDirectories(diretorio);
            Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("Falha ao salvar o arquivo: " + e.getMessage());
        }

        Curriculos curriculo = existente.orElseGet(Curriculos::new);
        curriculo.setCandidato(candidato);
        curriculo.setNomeArquivo(nomeOriginal);
        curriculo.setCaminhoArquivo(destino.toString());
        curriculo.setTamanhoArquivo(BigInteger.valueOf(arquivo.getSize()));
        curriculo.setFormatoArquivo(extensao.toUpperCase());

        Curriculos salvo = curriculoRepository.save(curriculo);

        candidato.setCurriculo(salvo);
        candidatoRepository.save(candidato);

        return curriculoMapper.toResponseDTO(salvo);
    }

    @Transactional(readOnly = true)
    public CurriculoResponseDTO buscar(Long usuarioId) {
        Candidatos candidato = candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de candidato não encontrado para o usuário ID: " + usuarioId));

        Curriculos curriculo = curriculoRepository.findByCandidatoId(candidato.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum currículo cadastrado."));

        return curriculoMapper.toResponseDTO(curriculo);
    }

    public ResponseEntity<Resource> download(Long usuarioId) {
        Candidatos candidato = candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de candidato não encontrado para o usuário ID: " + usuarioId));

        Curriculos curriculo = curriculoRepository.findByCandidatoId(candidato.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum currículo cadastrado."));

        Path arquivo = Paths.get(curriculo.getCaminhoArquivo());
        if (!Files.exists(arquivo)) {
            throw new ResourceNotFoundException("Arquivo não encontrado no servidor.");
        }

        Resource resource;
        try {
            resource = new UrlResource(arquivo.toUri());
        } catch (MalformedURLException e) {
            throw new BusinessException("Erro ao acessar o arquivo.");
        }

        String contentType = resolverContentType(curriculo.getFormatoArquivo());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + curriculo.getNomeArquivo() + "\"")
                .body(resource);
    }

    /**
     * Permite que uma empresa baixe o currículo de um candidato.
     *
     * Regras de acesso (todas devem ser satisfeitas):
     *  1. O usuarioId deve corresponder a uma empresa cadastrada.
     *  2. A candidatura deve pertencer a uma vaga dessa empresa.
     *  3. O candidato deve ter autorizado o compartilhamento do currículo
     *     (Candidatura.compartilharCurriculo == true).
     *  4. O candidato deve ter um currículo registrado com arquivo no disco.
     */
    public ResponseEntity<Resource> downloadParaEmpresa(Long candidaturaId, Long usuarioId) {

        // 1. Resolve a empresa a partir do usuário autenticado
        Empresas empresa = empresaRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new BusinessException(
                        "Nenhuma empresa vinculada a este usuário."));

        // 2. Carrega a candidatura
        Candidatura candidatura = candidaturaRepository.findById(candidaturaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Candidatura não encontrada com ID: " + candidaturaId));

        // 3. Garante que a vaga da candidatura pertence a esta empresa
        if (!candidatura.getVaga().getEmpresas().getId().equals(empresa.getId())) {
            throw new AccessDeniedException(
                    "Esta candidatura não pertence a uma vaga da sua empresa.");
        }

        // 4. Verifica se o candidato autorizou o compartilhamento do currículo
        if (!candidatura.isCompartilharCurriculo()) {
            throw new AccessDeniedException(
                    "O candidato não autorizou o compartilhamento do currículo nesta candidatura.");
        }

        // 5. Verifica se o candidato possui currículo registrado
        Curriculos curriculo = candidatura.getCandidato().getCurriculo();
        if (curriculo == null) {
            throw new ResourceNotFoundException(
                    "Este candidato não possui currículo cadastrado.");
        }

        // 6. Verifica se o arquivo existe fisicamente no servidor
        Path arquivo = Paths.get(curriculo.getCaminhoArquivo());
        if (!Files.exists(arquivo)) {
            throw new ResourceNotFoundException(
                    "Arquivo do currículo não encontrado no servidor.");
        }

        Resource resource;
        try {
            resource = new UrlResource(arquivo.toUri());
        } catch (MalformedURLException e) {
            throw new BusinessException("Erro ao acessar o arquivo.");
        }

        String contentType = resolverContentType(curriculo.getFormatoArquivo());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + curriculo.getNomeArquivo() + "\"")
                .body(resource);
    }

    @Transactional
    public void deletar(Long usuarioId) {
        Candidatos candidato = candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de candidato não encontrado para o usuário ID: " + usuarioId));

        Curriculos curriculo = curriculoRepository.findByCandidatoId(candidato.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum currículo para remover."));

        deletarArquivoDoDisco(curriculo.getCaminhoArquivo());

        candidato.setCurriculo(null);
        candidatoRepository.save(candidato);

        curriculoRepository.delete(curriculo);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

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
    }

    private void deletarArquivoDoDisco(String caminho) {
        if (caminho == null) return;
        try {
            Files.deleteIfExists(Paths.get(caminho));
        } catch (IOException ignored) {
            // log se necessário; não impede o fluxo
        }
    }

    private String resolverContentType(String formato) {
        if (formato == null) return "application/octet-stream";
        return switch (formato.toUpperCase()) {
            case "PDF"  -> "application/pdf";
            case "DOC"  -> "application/msword";
            case "DOCX" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default     -> "application/octet-stream";
        };
    }

    private String extrairExtensao(String nome) {
        int idx = nome.lastIndexOf('.');
        return idx >= 0 ? nome.substring(idx + 1).toLowerCase() : "pdf";
    }
}
