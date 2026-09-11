package com.matchvagas.backend.service.assistente;

import com.matchvagas.backend.entity.RecomendacaoTriagem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Fase 3 — recomendação da triagem assistida")
class RecomendacaoTriagemTest {

    @Test
    void aceitaVariacoesDeEscritaEValorForaDoCombinadoViraRevisar() {
        assertThat(TriagemCandidaturaService.recomendacao("não aderente")).isEqualTo(RecomendacaoTriagem.NAO_ADERENTE);
        assertThat(TriagemCandidaturaService.recomendacao("avancar")).isEqualTo(RecomendacaoTriagem.AVANCAR);
        assertThat(TriagemCandidaturaService.recomendacao("talvez")).isEqualTo(RecomendacaoTriagem.REVISAR);
        assertThat(TriagemCandidaturaService.recomendacao(null)).isEqualTo(RecomendacaoTriagem.REVISAR);
    }
}
