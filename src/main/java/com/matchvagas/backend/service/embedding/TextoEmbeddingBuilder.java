package com.matchvagas.backend.service.embedding;

import com.matchvagas.backend.entity.*;

import java.util.List;
import java.util.StringJoiner;

public final class TextoEmbeddingBuilder {
    private TextoEmbeddingBuilder() {}

    public static String daVaga(Vagas vaga) {
        StringJoiner texto = new StringJoiner(". ", "passage: ", "");
        add(texto, vaga.getTitulo());
        add(texto, vaga.getAreaAtuacao());
        if (vaga.getModalidade() != null) add(texto, vaga.getModalidade().getDescricao());
        if (vaga.getTipoVaga() != null) add(texto, vaga.getTipoVaga().getDescricao());
        if (vaga.getEscolaridade() != null) add(texto, vaga.getEscolaridade().getNome());
        add(texto, vaga.getDescricao());
        add(texto, vaga.getRequisitos());
        add(texto, vaga.getBeneficios());
        return texto.toString();
    }

    public static String doCandidato(Candidatos candidato, List<Experiencia> experiencias,
                                     List<Formacao> formacoes) {
        StringJoiner texto = new StringJoiner(". ", "query: ", "");
        add(texto, candidato.getObjetivoProfissional());
        add(texto, candidato.getDisponibilidade());
        if (candidato.getCurriculo() != null) add(texto, candidato.getCurriculo().getTextoExtraido());
        if (candidato.getHabilidades() != null)
            candidato.getHabilidades().forEach(h -> add(texto, h.getNome()));
        if (experiencias != null)
            experiencias.forEach(e -> { add(texto, e.getCargo()); add(texto, e.getDescricao()); });
        if (formacoes != null)
            formacoes.forEach(f -> { add(texto, f.getCurso()); add(texto, f.getInstituicao()); add(texto, f.getNivel()); });
        return texto.toString();
    }

    private static void add(StringJoiner texto, String valor) {
        if (valor != null && !valor.isBlank()) texto.add(valor.strip());
    }
}
