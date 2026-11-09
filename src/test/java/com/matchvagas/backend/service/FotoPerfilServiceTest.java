package com.matchvagas.backend.service;

import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Empresas;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.EmpresaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LGPD-08 — Foto de perfil e logo da empresa")
class FotoPerfilServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4};

    @Mock CandidatoRepository candidatoRepository;
    @Mock EmpresaRepository empresaRepository;
    @Mock SupabaseStorageService supabaseStorageService;
    @InjectMocks FotoPerfilService service;

    private MockMultipartFile png() {
        return new MockMultipartFile("arquivo", "Foto.PNG", "image/png", PNG);
    }

    @Test
    @DisplayName("upload do candidato guarda o caminho (não URL pública) e devolve URL assinada")
    void uploadCandidato() {
        Candidatos candidato = new Candidatos();
        candidato.setId(10L);
        when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));
        when(supabaseStorageService.gerarUrlAssinadaImagem(anyString(), eq(3600))).thenReturn("https://assinada");

        String url = service.uploadCandidato(1L, png());

        assertThat(url).isEqualTo("https://assinada");
        assertThat(candidato.getFotoPerfilUrl()).startsWith("candidatos/10/").endsWith(".png");
        verify(supabaseStorageService).uploadImagem(candidato.getFotoPerfilUrl(), PNG, "image/png");
        verify(candidatoRepository).save(candidato);
    }

    @Test
    @DisplayName("troca de foto remove a anterior; falha nessa remoção não impede o upload")
    void trocaFoto() {
        Candidatos candidato = new Candidatos();
        candidato.setId(10L);
        candidato.setFotoPerfilUrl("https://x.supabase.co/storage/v1/object/public/imagens-perfil/candidatos/10/velha.png");
        when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));
        doThrow(new BusinessException("storage fora")).when(supabaseStorageService).deletarImagem(anyString());

        service.uploadCandidato(1L, png());

        verify(supabaseStorageService).deletarImagem("candidatos/10/velha.png");
        assertThat(candidato.getFotoPerfilUrl()).doesNotContain("velha");
    }

    @Test
    @DisplayName("rejeita imagem de tipo não aceito ou com conteúdo forjado")
    void validaImagem() {
        assertThatThrownBy(() -> service.uploadCandidato(1L,
                new MockMultipartFile("arquivo", "a.gif", "image/gif", PNG)))
                .hasMessageContaining("Formato não aceito");
        assertThatThrownBy(() -> service.uploadCandidato(1L,
                new MockMultipartFile("arquivo", "a.jpg", "image/jpeg", PNG)))
                .hasMessageContaining("não corresponde");
        assertThatThrownBy(() -> service.uploadCandidato(1L, new MockMultipartFile("arquivo", new byte[0])))
                .hasMessageContaining("Nenhuma imagem");
        verifyNoInteractions(supabaseStorageService, candidatoRepository);
    }

    @Test
    @DisplayName("remove a foto do candidato; sem foto é erro")
    void deletaFotoCandidato() {
        Candidatos candidato = new Candidatos();
        candidato.setFotoPerfilUrl("candidatos/10/f.png");
        when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));

        service.deletarCandidato(1L);

        verify(supabaseStorageService).deletarImagem("candidatos/10/f.png");
        assertThat(candidato.getFotoPerfilUrl()).isNull();
        assertThatThrownBy(() -> service.deletarCandidato(1L)).hasMessageContaining("Nenhuma foto");
    }

    @Test
    @DisplayName("upload e remoção da logo da empresa")
    void logoEmpresa() {
        Empresas empresa = new Empresas();
        empresa.setId(20L);
        when(empresaRepository.findByUsuarioId(5L)).thenReturn(Optional.of(empresa));
        when(supabaseStorageService.gerarUrlAssinadaImagem(anyString(), anyInt())).thenReturn("https://logo");

        assertThat(service.uploadEmpresa(5L, png())).isEqualTo("https://logo");
        assertThat(empresa.getLogoUrl()).startsWith("empresas/20/");

        service.deletarEmpresa(5L);
        assertThat(empresa.getLogoUrl()).isNull();
        assertThatThrownBy(() -> service.deletarEmpresa(5L)).hasMessageContaining("Nenhuma logo");
    }

    @Test
    @DisplayName("usuário sem perfil ou sem empresa retorna 404")
    void semPerfil() {
        when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());
        when(empresaRepository.findByUsuarioId(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.uploadCandidato(1L, png())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.uploadEmpresa(5L, png())).isInstanceOf(ResourceNotFoundException.class);
        verify(supabaseStorageService, org.mockito.Mockito.never()).uploadImagem(anyString(), any(), anyString());
    }
}
