package br.org.amigosdonordeste.cadastro.familia;

import br.org.amigosdonordeste.cadastro.familia.dto.FamiliaFiltroDTO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import br.org.amigosdonordeste.cadastro.comum.BuscaPorNome;

import java.util.ArrayList;
import java.util.List;

/**
 * Issue #16: os filtros de GET /api/familias. Cada um só entra se veio na
 * requisição, então todos são opcionais e se combinam com AND. A mesma
 * Specification serve para a consulta da página e para o count.
 */
final class FamiliaEspecificacao {

    private FamiliaEspecificacao() { }

    static Specification<Familia> comFiltro(FamiliaFiltroDTO filtro) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            // Issue #43: inativa só aparece se pedirem
            if (!Boolean.TRUE.equals(filtro.incluirInativas())) {
                predicados.add(cb.isTrue(root.get("ativa")));
            }

            // sem acento, maiúscula nem apóstrofo dos dois lados (BuscaPorNome)
            if (filtro.busca() != null && !filtro.busca().isBlank()) {
                predicados.add(BuscaPorNome.contem(cb, root.get("responsavelNome"), filtro.busca()));
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

}
