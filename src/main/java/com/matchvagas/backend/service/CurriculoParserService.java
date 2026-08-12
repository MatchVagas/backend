package com.matchvagas.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchvagas.backend.dto.CurriculoParseResult;
import com.matchvagas.backend.exception.BusinessException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CurriculoParserService {
    private static final int LIMITE_CARACTERES = 200_000;
    private static final Pattern EMAIL = Pattern.compile("[\\w.!#$%&'*+/=?^`{|}~-]+@[\\w.-]+\\.[A-Za-z]{2,}");
    private static final Pattern TELEFONE = Pattern.compile("(?<!\\d)(?:\\+?55\\s*)?(?:\\(?\\d{2}\\)?\\s*)?9?\\d{4}[-.\\s]?\\d{4}(?!\\d)");
    private static final Set<String> COMPETENCIAS = Set.of(
            "java", "spring", "spring boot", "javascript", "typescript", "python", "php", "c#", ".net",
            "react", "angular", "vue", "node.js", "sql", "postgresql", "mysql", "oracle", "mongodb",
            "docker", "kubernetes", "aws", "azure", "gcp", "git", "jpa", "hibernate", "scrum", "kanban",
            "excel", "power bi", "figma", "linux", "rest", "graphql", "microserviços"
    );

    private final ObjectMapper objectMapper;

    public CurriculoParserService(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    public CurriculoParseResult parse(byte[] conteudo) {
        try (ByteArrayInputStream entrada = new ByteArrayInputStream(conteudo)) {
            BodyContentHandler handler = new BodyContentHandler(LIMITE_CARACTERES);
            new AutoDetectParser().parse(entrada, handler, new Metadata(), new ParseContext());
            String texto = normalizar(handler.toString());
            if (texto.isBlank()) throw new BusinessException("Não foi possível extrair texto do currículo.");
            List<String> linhas = Arrays.stream(texto.split("\\R")).map(String::strip).filter(s -> !s.isBlank()).toList();
            return new CurriculoParseResult(texto, detectarNome(linhas), primeiro(EMAIL, texto),
                    primeiro(TELEFONE, texto), detectarCompetencias(texto),
                    detectarSecao(linhas, "forma", "educa", "acad"),
                    detectarSecao(linhas, "experi", "histórico profissional"));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Falha ao processar o currículo: " + e.getMessage());
        }
    }

    public String toJson(CurriculoParseResult resultado) {
        try { return objectMapper.writeValueAsString(resultado); }
        catch (JsonProcessingException e) { throw new BusinessException("Falha ao estruturar dados do currículo."); }
    }

    private String normalizar(String texto) {
        return texto.replace('\u0000', ' ').replaceAll("[ \\t]+", " ")
                .replaceAll("(?:\\R\\s*){3,}", "\n\n").strip();
    }

    private String detectarNome(List<String> linhas) {
        return linhas.stream().limit(5)
                .filter(l -> l.length() >= 3 && l.length() <= 100)
                .filter(l -> !EMAIL.matcher(l).find() && !TELEFONE.matcher(l).find())
                .filter(l -> l.matches("[\\p{L}][\\p{L} .'-]+"))
                .findFirst().orElse(null);
    }

    private String primeiro(Pattern pattern, String texto) {
        Matcher matcher = pattern.matcher(texto);
        return matcher.find() ? matcher.group().strip() : null;
    }

    private List<String> detectarCompetencias(String texto) {
        String lower = texto.toLowerCase(Locale.ROOT);
        return COMPETENCIAS.stream().filter(c -> Pattern.compile("(?<![\\p{L}\\d])" + Pattern.quote(c)
                + "(?![\\p{L}\\d])", Pattern.CASE_INSENSITIVE).matcher(lower).find()).sorted().toList();
    }

    private List<String> detectarSecao(List<String> linhas, String... marcadores) {
        List<String> resultado = new ArrayList<>();
        boolean dentro = false;
        for (String linha : linhas) {
            String lower = linha.toLowerCase(Locale.ROOT);
            if (!dentro && Arrays.stream(marcadores).anyMatch(lower::contains)) { dentro = true; continue; }
            if (dentro && pareceTitulo(lower)) break;
            if (dentro && resultado.size() < 20) resultado.add(linha);
        }
        return List.copyOf(new LinkedHashSet<>(resultado));
    }

    private boolean pareceTitulo(String linha) {
        return linha.length() < 50 && (linha.contains("habilidade") || linha.contains("competência")
                || linha.contains("idioma") || linha.contains("objetivo") || linha.contains("resumo"));
    }
}
