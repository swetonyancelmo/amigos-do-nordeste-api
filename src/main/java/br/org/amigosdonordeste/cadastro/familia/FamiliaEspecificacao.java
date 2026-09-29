package br.org.amigosdonordeste.cadastro.familia;

import br.org.amigosdonordeste.cadastro.familia.dto.FamiliaFiltroDTO;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Issue #16: os filtros de GET /api/familias. Cada um só entra se veio na
 * requisição, então todos são opcionais e se combinam com AND. A mesma
 * Specification serve para a consulta da página e para o count.
 */
final class FamiliaEspecificacao {

    private static final char ESCAPE = '\\';

    private FamiliaEspecificacao() { }

    static Specification<Familia> comFiltro(FamiliaFiltroDTO filtro) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            // Issue #43: inativa só aparece se pedirem
            if (!Boolean.TRUE.equals(filtro.incluirInativas())) {
                predicados.add(cb.isTrue(root.get("ativa")));
            }

            // unaccent (extensão da V1) dos dois lados: "Jose" acha "José" e
            // vice-versa. No H2 dos testes, o alias UNACCENT de application-test.yml.
            if (filtro.busca() != null && !filtro.busca().isBlank()) {
                String termo = "%" + escaparCuringas(filtro.busca().trim()) + "%";
                Expression<String> nome = cb.function("unaccent", String.class, cb.lower(root.get("responsavelNome")));
                Expression<String> busca = cb.function("unaccent", String.class, cb.lower(cb.literal(termo)));
                predicados.add(cb.like(nome, busca, ESCAPE));
            }

            if (filtro.municipioId() != null) {
                predicados.add(cb.equal(root.get("comunidade").get("municipio").get("id"), filtro.municipioId()));
            }

            if (filtro.comunidadeId() != null) {
                predicados.add(cb.equal(root.get("comunidade").get("id"), filtro.comunidadeId()));
            }

            // Mesmo critério do indicador do dashboard (contarSemBanheiro):
            // não informado conta como sem banheiro
            if (Boolean.TRUE.equals(filtro.semBanheiro())) {
                predicados.add(cb.or(cb.isNull(root.get("temBanheiro")), cb.isFalse(root.get("temBanheiro"))));
            }

            return cb.and(predicados.toArray(new Predicate[0]));
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
