package com.matchvagas.backend.repository;

import com.matchvagas.backend.dto.VagaBuscaFiltro;
import com.matchvagas.backend.entity.Vagas;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fase 2 — VagaSpecs (busca portátil, sem full-text de banco)")
class VagaSpecsTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS) Root<Vagas> root;
    @Mock CriteriaQuery<?> query;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) CriteriaBuilder cb;

    private Predicate[] predicadosDe(VagaBuscaFiltro filtro) {
        VagaSpecs.comFiltros(filtro).toPredicate(root, query, cb);
        ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
        verify(cb).and(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Sem filtros e apenasAtivas=false não gera nenhum predicado")
    void semFiltros() {
        VagaBuscaFiltro filtro = new VagaBuscaFiltro(
                null, null, null, null, null, null, null, null, null, null, false);

        assertThat(predicadosDe(filtro)).isEmpty();
    }

    @Test
    @DisplayName("apenasAtivas=true adiciona status ativa + não expirada (2 predicados)")
    void apenasAtivas() {
        VagaBuscaFiltro filtro = new VagaBuscaFiltro(
                null, null, null, null, null, null, null, null, null, null, true);

        assertThat(predicadosDe(filtro)).hasSize(2);
    }

    @Test
    @DisplayName("Texto em branco não vira predicado")
    void textoEmBrancoIgnorado() {
        VagaBuscaFiltro filtro = new VagaBuscaFiltro(
                "   ", "", null, null, null, null, null, "  ", null, null, false);

        assertThat(predicadosDe(filtro)).isEmpty();
    }

    @Test
    @DisplayName("Cada critério informado gera um predicado")
    void filtrosCombinados() {
        VagaBuscaFiltro filtro = new VagaBuscaFiltro(
                "java", "tec", 1L, 2L, 3L, 4L, 5L, "corp",
                new BigDecimal("3000"), new BigDecimal("9000"), false);

        // termo + area + nomeEmpresa + tipo + modalidade + escolaridade
        // + cidade + estado + salarioMin + salarioMax = 10
        assertThat(predicadosDe(filtro)).hasSize(10);
    }
}
