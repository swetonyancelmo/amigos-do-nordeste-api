package br.org.amigosdonordeste.cadastro.pessoa;

import br.org.amigosdonordeste.cadastro.pessoa.dto.PessoaFiltroDTO;
import br.org.amigosdonordeste.cadastro.pessoa.enums.FaixaEtaria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Os filtros de GET /api/pessoas. Cada um só entra se veio na requisição; a
 * mesma Specification serve para a página e para o count — por isso a faixa
 * etária tem que ser calculada aqui, no banco: filtrar em memória depois
 * faria o total e a paginação mentirem.
 */
final class PessoaEspecificacao {

    private static final char ESCAPE = '\\';

    private PessoaEspecificacao() { }

    static Specification<Pessoa> comFiltro(PessoaFiltroDTO filtro, LocalDate hoje) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            Path<Object> familia = root.get("familia");
            Path<Object> comunidade = familia.get("comunidade");

            // Issue #43: família inativa some de listagem e contagem — os
            // membros dela também
            predicados.add(cb.isTrue(familia.get("ativa")));

            // unaccent dos dois lados: o cadastro vem de papel, "Jose" tem que
            // achar "José". No H2 dos testes, o alias UNACCENT de application-test.yml.
            if (filtro.nome() != null && !filtro.nome().isBlank()) {
                String termo = "%" + escaparCuringas(filtro.nome().trim()) + "%";
                Expression<String> nome = cb.function("unaccent", String.class, cb.lower(root.get("nome")));
                Expression<String> busca = cb.function("unaccent", String.class, cb.lower(cb.literal(termo)));
                predicados.add(cb.like(nome, busca, ESCAPE));
            }

            if (filtro.familiaId() != null) {
                predicados.add(cb.equal(familia.get("id"), filtro.familiaId()));
            }
            if (filtro.comunidadeId() != null) {
                predicados.add(cb.equal(comunidade.get("id"), filtro.comunidadeId()));
            }
            if (filtro.municipioId() != null) {
                predicados.add(cb.equal(comunidade.get("municipio").get("id"), filtro.municipioId()));
            }
            if (filtro.cadastroIncompleto() != null) {
                predicados.add(cb.equal(root.get("cadastroIncompleto"), filtro.cadastroIncompleto()));
            }
            if (filtro.estuda() != null) {
                predicados.add(cb.equal(root.get("estuda"), filtro.estuda()));
            }
            if (filtro.faixaEtaria() != null) {
                predicados.add(naFaixa(cb, idade(cb, root, hoje), filtro.faixaEtaria()));
            }

            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    /**
     * A mesma conta de dominio.Idade, em SQL: data de nascimento quando
     * existe; senão idade estimada + anos completos desde a estimativa. Data
     * no futuro ou falta de informação dá null — e null não cai em faixa
     * nenhuma, igual a FaixaEtaria.de(null).
     */
    private static Expression<Integer> idade(CriteriaBuilder cb, Path<Pessoa> root, LocalDate hoje) {
        Path<LocalDate> dataNascimento = root.get("dataNascimento");
        Path<Integer> idadeEstimada = root.get("idadeEstimada");
        Path<LocalDate> idadeEstimadaEm = root.get("idadeEstimadaEm");

        return cb.<Integer>selectCase()
                .when(cb.and(
                                cb.isNotNull(dataNascimento),
                                cb.lessThanOrEqualTo(dataNascimento, hoje)),
                        anosCompletos(cb, dataNascimento, hoje))
                .when(cb.and(
                                cb.isNull(dataNascimento),
                                cb.isNotNull(idadeEstimada),
                                cb.isNotNull(idadeEstimadaEm),
                                cb.lessThanOrEqualTo(idadeEstimadaEm, hoje)),
                        cb.sum(idadeEstimada, anosCompletos(cb, idadeEstimadaEm, hoje)))
                .otherwise(cb.nullLiteral(Integer.class));
    }

    /**
     * Anos completos de {@code desde} até {@code hoje} — o getYears() de
     * Period.between: diferença dos anos, menos um se o aniversário deste ano
     * ainda não chegou. Feito com year/month/day (extract) para rodar igual no
     * Postgres e no H2 dos testes.
     */
    private static Expression<Integer> anosCompletos(CriteriaBuilder cb, Path<LocalDate> desde, LocalDate hoje) {
        Expression<Integer> ano = cb.function("year", Integer.class, desde);
        Expression<Integer> mes = cb.function("month", Integer.class, desde);
        Expression<Integer> dia = cb.function("day", Integer.class, desde);

        Expression<Integer> aniversarioAindaNaoChegou = cb.<Integer>selectCase()
                .when(cb.or(
                                cb.gt(mes, hoje.getMonthValue()),
                                cb.and(cb.equal(mes, hoje.getMonthValue()), cb.gt(dia, hoje.getDayOfMonth()))),
                        1)
                .otherwise(0);

        return cb.diff(cb.diff(cb.literal(hoje.getYear()), ano), aniversarioAindaNaoChegou);
    }

    /** Mesmos limites de FaixaEtaria.de(). */
    private static Predicate naFaixa(CriteriaBuilder cb, Expression<Integer> idade, FaixaEtaria faixa) {
        return switch (faixa) {
            case ATE_12 -> cb.le(idade, 12);
            case DE_13_A_59 -> cb.between(idade, 13, 59);
            case DE_60_OU_MAIS -> cb.ge(idade, 60);
        };
    }

    /** "%" e "_" digitados na busca são texto, não curinga do LIKE. */
    private static String escaparCuringas(String texto) {
        return texto
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
