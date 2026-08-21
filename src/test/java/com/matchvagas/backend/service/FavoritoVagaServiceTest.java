package com.matchvagas.backend.service;

import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.FavoritoVaga;
import com.matchvagas.backend.entity.Vagas;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.VagasMapper;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.FavoritoVagaRepository;
import com.matchvagas.backend.repository.VagaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fase 2 — Favoritos de vaga")
class FavoritoVagaServiceTest {

    @Mock FavoritoVagaRepository favoritoRepository;
    @Mock CandidatoRepository    candidatoRepository;
    @Mock VagaRepository         vagaRepository;
    @Mock VagasMapper            vagasMapper;

    @InjectMocks FavoritoVagaService service;

    private static final Long USUARIO_ID   = 1L;
    private static final Long CANDIDATO_ID = 10L;
    private static final Long VAGA_ID      = 5L;
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    private Candidatos candidato;
    private Vagas vaga;

    @BeforeEach
    void setUp() {
        candidato = new Candidatos();
        candidato.setId(CANDIDATO_ID);
        vaga = new Vagas();
        vaga.setId(VAGA_ID);
    }

    @Test
    @DisplayName("Favoritar nova vaga persiste o favorito")
    void favoritarNova() {
        when(candidatoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(candidato));
        when(vagaRepository.findById(VAGA_ID)).thenReturn(Optional.of(vaga));
        when(favoritoRepository.existsByCandidatoIdAndVagaId(CANDIDATO_ID, VAGA_ID)).thenReturn(false);

        service.favoritar(USUARIO_ID, VAGA_ID);

        verify(favoritoRepository).save(any(FavoritoVaga.class));
        verify(vagasMapper).toDTO(vaga);
    }

    @Test
    @DisplayName("Favoritar é idempotente: não duplica se já favoritada")
    void favoritarIdempotente() {
        when(candidatoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(candidato));
        when(vagaRepository.findById(VAGA_ID)).thenReturn(Optional.of(vaga));
        when(favoritoRepository.existsByCandidatoIdAndVagaId(CANDIDATO_ID, VAGA_ID)).thenReturn(true);

        service.favoritar(USUARIO_ID, VAGA_ID);

        verify(favoritoRepository, never()).save(any());
        verify(vagasMapper).toDTO(vaga);
    }

    @Test
    @DisplayName("Favoritar vaga inexistente → ResourceNotFoundException")
    void favoritarVagaInexistente() {
        when(candidatoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(candidato));
        when(vagaRepository.findById(VAGA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.favoritar(USUARIO_ID, VAGA_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(favoritoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Desfavoritar remove o favorito existente")
    void desfavoritar() {
        FavoritoVaga favorito = new FavoritoVaga();
        favorito.setId(99L);
        when(candidatoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(candidato));
        when(favoritoRepository.findByCandidatoIdAndVagaId(CANDIDATO_ID, VAGA_ID))
                .thenReturn(Optional.of(favorito));

        service.desfavoritar(USUARIO_ID, VAGA_ID);

        verify(favoritoRepository).delete(favorito);
    }

    @Test
    @DisplayName("Desfavoritar vaga que não é favorita → ResourceNotFoundException")
    void desfavoritarInexistente() {
        when(candidatoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(candidato));
        when(favoritoRepository.findByCandidatoIdAndVagaId(CANDIDATO_ID, VAGA_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.desfavoritar(USUARIO_ID, VAGA_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(favoritoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Listar devolve as vagas favoritadas paginadas")
    void listar() {
        FavoritoVaga favorito = new FavoritoVaga();
        favorito.setVaga(vaga);
        when(candidatoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(candidato));
        when(favoritoRepository.findByCandidatoIdOrderByDataFavoritadoDesc(CANDIDATO_ID, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(favorito), PAGEABLE, 1));

        var result = service.listar(USUARIO_ID, PAGEABLE);

        assertThat(result.content()).hasSize(1);
        verify(vagasMapper).toDTO(vaga);
    }
}
