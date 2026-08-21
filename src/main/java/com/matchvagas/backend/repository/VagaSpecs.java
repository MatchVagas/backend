package com.matchvagas.backend.repository;

import com.matchvagas.backend.dto.VagaBuscaFiltro;
import com.matchvagas.backend.entity.Vagas;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Specifications (Criteria API) da busca de vagas. Portátil entre Postgres e MySQL: usa só
 * predicados padrão (LIKE/ranges/joins), sem full-text específico de banco (tsvector /
 * MATCH ... AGAINST). Filtro ausente simplesmente não vira predicado — evitando também o
 * problema de inferência de tipo do Postgres com parâmetros nulos.
 */
public final class VagaSpecs {

    private VagaSpecs() {}

    public static Specification<Vagas> comFiltros(VagaBuscaFiltro f) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            if (temTexto(f.termo())) {
                String like = curinga(f.termo());
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("titulo")),      like),
                        cb.like(cb.lower(root.get("descricao")),   like),
                        cb.like(cb.lower(root.get("requisitos")),  like),
                        cb.like(cb.lower(root.get("areaAtuacao")), like)));
            }
            if (temTexto(f.areaAtuacao()))
                ps.add(cb.like(cb.lower(root.get("areaAtuacao")), curinga(f.areaAtuacao())));
            if (temTexto(f.nomeEmpresa()))
                ps.add(cb.like(cb.lower(root.get("empresas").get("nomeFantasia")), curinga(f.nomeEmpresa())));

            if (f.tipoVagaId() != null)
                ps.add(cb.equal(root.get("tipoVaga").get("id"), f.tipoVagaId()));
            if (f.modalidadeId() != null)
                ps.add(cb.equal(root.get("modalidade").get("id"), f.modalidadeId()));
            if (f.escolaridadeId() != null)
                ps.add(cb.equal(root.get("escolaridade").get("id"), f.escolaridadeId()));
            if (f.cidadeId() != null)
                ps.add(cb.equal(root.get("cidade").get("id"), f.cidadeId()));
            if (f.estadoId() != null)
                ps.add(cb.equal(root.get("cidade").get("estado").get("id"), f.estadoId()));

            // Faixa salarial como sobreposição de intervalos [salarioMinimo, salarioMaximo].
            if (f.salarioMin() != null)
                ps.add(cb.greaterThanOrEqualTo(root.get("salarioMaximo"), f.salarioMin()));
            if (f.salarioMax() != null)
                ps.add(cb.lessThanOrEqualTo(root.get("salarioMinimo"), f.salarioMax()));

            if (f.apenasAtivas()) {
                ps.add(cb.equal(cb.lower(root.get("status").get("descricao")), "ativa"));
                ps.add(cb.greaterThanOrEqualTo(root.get("dataExpiracao"), LocalDateTime.now()));
            }

            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private static boolean temTexto(String s) {
        return s != null && !s.isBlank();
    }

    private static String curinga(String s) {
        return "%" + s.trim().toLowerCase() + "%";
    }
}
