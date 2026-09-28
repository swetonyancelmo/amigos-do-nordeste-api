package br.org.amigosdonordeste.cadastro.familia.repository;

import br.org.amigosdonordeste.cadastro.familia.Familia;
import br.org.amigosdonordeste.cadastro.familia.dto.FamiliaFiltroDTO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class FamiliaEspecificacao {

  public static Specification<Familia> comFiltro(FamiliaFiltroDTO filtro) {
    return (root, query, cb) -> {
      List<Predicate> predicados = new ArrayList<>();

      // 1. Busca textual com unaccent no nome da responsável
      if (filtro.busca() != null && !filtro.busca().isBlank()) {
        var termo = "%" + filtro.busca().trim().toLowerCase() + "%";
        var nomeSemAcento = cb.function("unaccent", String.class, cb.lower(root.get("responsavelNome")));
        var termoSemAcento = cb.function("unaccent", String.class, cb.literal(termo));
        predicados.add(cb.like(nomeSemAcento, termoSemAcento));
      }

      // 2. Filtro por Município (através de comunidade.municipio.id)
      if (filtro.municipioId() != null) {
        var comunidade = root.join("comunidade");
        var municipio = comunidade.join("municipio");
        predicados.add(cb.equal(municipio.get("id"), filtro.municipioId()));
      }

      // 3. Filtro por Comunidade
      if (filtro.comunidadeId() != null) {
        predicados.add(cb.equal(root.get("comunidade").get("id"), filtro.comunidadeId()));
      }

      // 4. Filtro rápido semBanheiro (semBanheiro=true -> temBanheiro=false)
      if (filtro.semBanheiro() != null) {
        predicados.add(cb.equal(root.get("temBanheiro"), !filtro.semBanheiro()));
      }

      return cb.and(predicados.toArray(new Predicate[0]));
    };
  }
}
