package com.matchvagas.backend.service.embedding;

import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Curriculos;
import com.matchvagas.backend.entity.Experiencia;
import com.matchvagas.backend.entity.Formacao;
import com.matchvagas.backend.entity.Habilidade;
import com.matchvagas.backend.entity.Modalidade;
import com.matchvagas.backend.entity.Vagas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Fase 3 — texto indexado nos embeddings")
class TextoEmbeddingBuilderTest {

    @Test
    @DisplayName("vaga vira 'passage:' com os campos preenchidos, sem vazios")
    void textoDaVaga() {
        Vagas vaga = new Vagas();
        vaga.setTitulo(" Desenvolvedor Java ");
        vaga.setAreaAtuacao("Tecnologia");
        Modalidade modalidade = new Modalidade();
        modalidade.setDescricao("Remoto");
        vaga.setModalidade(modalidade);
        vaga.setDescricao("   ");
        vaga.setRequisitos("Spring Boot");

        assertThat(TextoEmbeddingBuilder.daVaga(vaga))
                .isEqualTo("passage: Desenvolvedor Java. Tecnologia. Remoto. Spring Boot");
    }

    @Test
    @DisplayName("candidato vira 'query:' com perfil, currículo, habilidades, experiências e formações")
    void textoDoCandidato() {
        Candidatos candidato = new Candidatos();
        candidato.setObjetivoProfissional("Atuar como backend");
        Curriculos curriculo = new Curriculos();
        curriculo.setTextoExtraido("Texto do currículo");
        candidato.setCurriculo(curriculo);
        Habilidade habilidade = new Habilidade();
        habilidade.setNome("Java");
        candidato.setHabilidades(List.of(habilidade));
        Experiencia experiencia = new Experiencia();
        experiencia.setCargo("Dev");
        experiencia.setDescricao("APIs REST");
        Formacao formacao = new Formacao();
        formacao.setCurso("Computação");
        formacao.setInstituicao("UFX");

        assertThat(TextoEmbeddingBuilder.doCandidato(candidato, List.of(experiencia), List.of(formacao)))
                .isEqualTo("query: Atuar como backend. Texto do currículo. Java. Dev. APIs REST. Computação. UFX");
    }

    @Test
    @DisplayName("candidato sem dados gera só o prefixo, e listas nulas são aceitas")
    void candidatoVazio() {
        Candidatos candidato = new Candidatos();
        candidato.setHabilidades(null);

        assertThat(TextoEmbeddingBuilder.doCandidato(candidato, null, null)).isEqualTo("query: ");
    }
}
