package br.org.amigosdonordeste.cadastro.familia.dto;

import java.util.UUID;

public record FamiliaFiltroDTO(
  String busca,
  UUID municipioId,
  UUID comunidadeId,
  Boolean semBanheiro,
  Integer pagina,
  Integer porPagina
) {
  public int paginaNormalizada() {
    return (pagina == null || pagina < 0) ? 0 : pagina;
  }

  public int porPaginaNormalizada() {
    if (porPagina == null || porPagina <= 0) {
      return 25; // padrão 25
    }
    return Math.min(porPagina, 100); // teto 100
  }
}
