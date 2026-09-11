package com.matchvagas.backend.service;

import com.matchvagas.backend.entity.CandidatoEmbedding;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Empresas;
import com.matchvagas.backend.entity.Habilidade;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.entity.VagaEmbedding;
import com.matchvagas.backend.entity.Vagas;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.repository.CandidatoEmbeddingRepository;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.CandidaturaRepository;
import com.matchvagas.backend.repository.EmpresaRepository;
import com.matchvagas.backend.repository.VagaEmbeddingRepository;
import com.matchvagas.backend.repository.VagaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fase 3 — recomendação de candidatos para a empresa")
class RecomendacaoCandidatoServiceTest {
    @Mock VagaRepository vagaRepository;
    @Mock EmpresaRepository empresaRepository;
    @Mock CandidatoRepository candidatoRepository;
    @Mock CandidaturaRepository candidaturaRepository;
    @Mock VagaEmbeddingRepository vagaEmbeddingRepository;
    @Mock CandidatoEmbeddingRepository candidatoEmbeddingRepository;

    private RecomendacaoCandidatoService service;

    @BeforeEach
    void setUp() {
        service = new RecomendacaoCandidatoService(vagaRepository, empresaRepository,
                candidatoRepository, candidaturaRepository, vagaEmbeddingRepository,
                candidatoEmbeddingRepository);
        ReflectionTestUtils.setField(service, "limiarSemantico", 0.30);
        ReflectionTestUtils.setField(service, "embeddingsAtivos", false);
    }

    @Test
    void recomendaSomenteOptInComDadosProfissionaisMinimos() {
        Empresas empresa = empresa(10L);
        Vagas vaga = vaga(20L, empresa);
        Candidatos candidato = candidato(30L, "Desenvolvedor Java", true);
        when(vagaRepository.findById(20L)).thenReturn(Optional.of(vaga));
        when(empresaRepository.findByUsuarioId(5L)).thenReturn(Optional.of(empresa));
        when(candidaturaRepository.findByVagaId(20L)).thenReturn(List.of());
        when(candidatoRepository.findByDisponivelParaRecomendacoesTrue()).thenReturn(List.of(candidato));

        var pagina = service.recomendar(20L, 5L, PageRequest.of(0, 20));

        assertThat(pagina.totalElements()).isEqualTo(1);
        assertThat(pagina.content().getFirst().candidatoId()).isEqualTo(30L);
        assertThat(pagina.content().getFirst().habilidades()).containsExactly("Java");
        assertThat(pagina.content().getFirst().pontuacao()).isGreaterThan(15);
    }

    @Test
    void usaEmbeddingsPersistidosSemExecutarModelo() {
        ReflectionTestUtils.setField(service, "embeddingsAtivos", true);
        Empresas empresa = empresa(10L);
        Vagas vaga = vaga(20L, empresa);
        Candidatos candidato = candidato(30L, "Outra área", true);
        VagaEmbedding vagaEmbedding = new VagaEmbedding();
        vagaEmbedding.setVaga(vaga);
        vagaEmbedding.setVetor("1.0,0.0");
        vagaEmbedding.setModelo("modelo");
        vagaEmbedding.setDim(2);
        CandidatoEmbedding candidatoEmbedding = new CandidatoEmbedding();
        candidatoEmbedding.setCandidato(candidato);
        candidatoEmbedding.setVetor("1.0,0.0");
        candidatoEmbedding.setModelo("modelo");
        candidatoEmbedding.setDim(2);
        when(vagaRepository.findById(20L)).thenReturn(Optional.of(vaga));
        when(empresaRepository.findByUsuarioId(5L)).thenReturn(Optional.of(empresa));
        when(candidaturaRepository.findByVagaId(20L)).thenReturn(List.of());
        when(candidatoRepository.findByDisponivelParaRecomendacoesTrue()).thenReturn(List.of(candidato));
        when(vagaEmbeddingRepository.findByVagaId(20L)).thenReturn(Optional.of(vagaEmbedding));
        when(candidatoEmbeddingRepository.findByCandidatoIdIn(List.of(30L)))
                .thenReturn(List.of(candidatoEmbedding));

        var item = service.recomendar(20L, 5L, PageRequest.of(0, 20)).content().getFirst();

        assertThat(item.pontuacao()).isEqualTo(100);
        assertThat(item.motivos()).anyMatch(m -> m.contains("100%"));
    }

    @Test
    void bloqueiaVagaDeOutraEmpresa() {
        Vagas vaga = vaga(20L, empresa(99L));
        when(vagaRepository.findById(20L)).thenReturn(Optional.of(vaga));
        when(empresaRepository.findByUsuarioId(5L)).thenReturn(Optional.of(empresa(10L)));

        assertThatThrownBy(() -> service.recomendar(20L, 5L, PageRequest.of(0, 20)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("permissão");
    }

    private Empresas empresa(Long id) {
        Empresas empresa = new Empresas();
        empresa.setId(id);
        return empresa;
    }

    private Vagas vaga(Long id, Empresas empresa) {
        Vagas vaga = new Vagas();
        vaga.setId(id);
        vaga.setEmpresas(empresa);
        vaga.setTitulo("Desenvolvedor Java");
        vaga.setAreaAtuacao("Tecnologia Java");
        vaga.setRequisitos("Java Spring");
        vaga.setSalarioMinimo(new BigDecimal("5000"));
        vaga.setSalarioMaximo(new BigDecimal("8000"));
        vaga.setIdadeMinima(18);
        vaga.setIdadeMaxima(65);
        return vaga;
    }

    private Candidatos candidato(Long id, String objetivo, boolean optIn) {
        Usuarios usuario = new Usuarios();
        usuario.setAtivo(true);
        usuario.setIdade(30);
        Habilidade habilidade = new Habilidade();
        habilidade.setNome("Java");
        Candidatos candidato = new Candidatos();
        candidato.setId(id);
        candidato.setUsuario(usuario);
        candidato.setObjetivoProfissional(objetivo);
        candidato.setDisponibilidade("Imediata");
        candidato.setPretensaoSalarial(new BigDecimal("6000"));
        candidato.setHabilidades(List.of(habilidade));
        candidato.setDisponivelParaRecomendacoes(optIn);
        return candidato;
    }
}
