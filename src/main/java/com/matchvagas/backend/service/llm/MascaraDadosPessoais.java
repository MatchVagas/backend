package com.matchvagas.backend.service.llm;

import java.util.regex.Pattern;

/**
 * Minimização (LGPD art. 6º, III): remove identificadores diretos do texto antes de
 * enviá-lo ao LLM. O modelo precisa das qualificações, não de como contatar a pessoa.
 */
public final class MascaraDadosPessoais {
    private static final Pattern EMAIL = Pattern.compile("[\\w.!#$%&'*+/=?^`{|}~-]+@[\\w.-]+\\.[A-Za-z]{2,}");
    private static final Pattern URL_PERFIL = Pattern.compile(
            "(?i)(?:https?://)?(?:www\\.)?(?:linkedin|github|instagram|facebook)\\.com/\\S+");
    // Exige DDD, para não confundir períodos como "2019-2021" com telefone.
    private static final Pattern TELEFONE = Pattern.compile(
            "(?<!\\d)(?:\\+?55\\s?)?\\(?\\d{2}\\)?[\\s-]?9?\\d{4}[\\s.-]?\\d{4}(?!\\d)");
    private static final Pattern CPF = Pattern.compile("(?<!\\d)\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}(?!\\d)");

    private MascaraDadosPessoais() {}

    public static String mascarar(String texto) {
        if (texto == null) return null;
        String r = EMAIL.matcher(texto).replaceAll("[e-mail omitido]");
        r = URL_PERFIL.matcher(r).replaceAll("[perfil omitido]");
        r = TELEFONE.matcher(r).replaceAll("[telefone omitido]");
        return CPF.matcher(r).replaceAll("[CPF omitido]");
    }
}