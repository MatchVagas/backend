package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.HabilidadeRequestDTO;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Habilidade;
import com.matchvagas.backend.entity.NivelHabilidade;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.HabilidadeMapper;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.service.embedding.IndexacaoEmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RF003 — Habilidades do candidato")
class HabilidadeServiceTest {

    @Mock CandidatoRepository candidatoRepository;
    @Spy HabilidadeMapper habilidadeMapper = Mappers.getMapper(HabilidadeMapper.class);
    @Mock IndexacaoEmbeddingService indexacaoEmbeddingService;
    @Spy AposCommitExecutor aposCommitExecutor = new AposCommitExecutor();
    @InjectMocks HabilidadeService service;

    private Candidatos candidato;

    @BeforeEach
    void setUp() {
        Habilidade java = new Habilidade();
        java.setNome("Java");
        java.setNivel(NivelHabilidade.INTERMEDIARIO);
        candidato = new Candidatos();
        candidato.setId(10L);
        candidato.setHabilidades(new ArrayList<>(List.of(java)));
        when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));
    }

    @Test
    @DisplayName("lista as habilidades do candidato")
    void lista() {
        assertThat(service.listar(1L)).extracting(h -> h.nome()).containsExactly("Java");
    }

    @Test
    @DisplayName("adiciona nova habilidade e reindexa o perfil")
    void adiciona() {
        var resposta = service.adicionar(1L, new HabilidadeRequestDTO("Docker", NivelHabilidade.BASICO));

        assertThat(resposta.nome()).isEqualTo("Docker");
        assertThat(candidato.getHabilidades()).extracting(Habilidade::getNome).containsExactly("Java", "Docker");
        verify(candidatoRepository).save(candidato);
        verify(indexacaoEmbeddingService).indexarCandidato(candidato);
    }

    @Test
    @DisplayName("rejeita habilidade repetida, ignorando maiúsculas")
    void rejeitaDuplicada() {
        assertThatThrownBy(() -> service.adicionar(1L, new HabilidadeRequestDTO("JAVA", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já cadastrada");
        verify(candidatoRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualiza nome e nível; renomear para nome existente é rejeitado")
    void atualiza() {
        var resposta = service.atualizar(1L, "java", new HabilidadeRequestDTO("Java", NivelHabilidade.BASICO));
        assertThat(resposta.nivel()).isEqualTo(NivelHabilidade.BASICO);

        candidato.getHabilidades().add(habilidade("Spring"));
        assertThatThrownBy(() -> service.atualizar(1L, "Spring", new HabilidadeRequestDTO("java", null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("remove pelo nome; nome inexistente retorna 404")
    void remove() {
        service.remover(1L, "JAVA");
        assertThat(candidato.getHabilidades()).isEmpty();
        verify(indexacaoEmbeddingService).indexarCandidato(candidato);

        assertThatThrownBy(() -> service.remover(1L, "Cobol")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.atualizar(1L, "Cobol", new HabilidadeRequestDTO("X", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Habilidade habilidade(String nome) {
        Habilidade h = new Habilidade();
        h.setNome(nome);
        return h;
    }
}
