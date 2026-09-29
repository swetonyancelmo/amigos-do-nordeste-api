package br.org.amigosdonordeste.cadastro.familia.dto;

import java.util.UUID;

/**
 * Issue #16: parâmetros de GET /api/familias. Todos opcionais e combináveis.
 *
 * semBanheiro é filtro rápido: só true filtra (false ou ausente = sem filtro).
 * incluirInativas vem da issue #43 — é o único jeito de achar uma família
 * inativa para reativar.
 */
public record FamiliaFiltroDTO(
    String busca,
    UUID municipioId,
    UUID comunidadeId,
    Boolean semBanheiro,
    Boolean incluirInativas,
    Integer pagina,
    Integer porPagina
) {
    public static final int POR_PAGINA_PADRAO = 25;
    public static final int POR_PAGINA_MAXIMO = 100;

    public int paginaNormalizada() {
        return (pagina == null || pagina < 0) ? 0 : pagina;
    }

    public int porPaginaNormalizada() {
        if (porPagina == null || porPagina <= 0) {
            return POR_PAGINA_PADRAO;
        }
        return Math.min(porPagina, POR_PAGINA_MAXIMO);
    }
}
