package br.org.amigosdonordeste.cadastro.familia.dto;

import java.util.List;
import java.util.UUID;

import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.EstratoRisco;

/**
 * Issue #16: parâmetros de GET /api/familias. Todos opcionais e combináveis.
 *
 * semBanheiro é filtro rápido: só true filtra (false ou ausente = sem filtro).
 * incluirInativas vem da issue #43 — é o único jeito de achar uma família
 * inativa para reativar.
 *
 * estrato e ordenacao vêm da avaliação de vulnerabilidade (ADR-0010).
 * estrato aceita mais de um (estrato=R3&estrato=DADOS_INSUFICIENTES).
 */
public record FamiliaFiltroDTO(
    String busca,
    UUID municipioId,
    UUID comunidadeId,
    Boolean semBanheiro,
    Boolean incluirInativas,
    Integer pagina,
    Integer porPagina,
    List<EstratoRisco> estrato,
    OrdenacaoFamilia ordenacao
) {
    public static final int POR_PAGINA_PADRAO = 25;
    public static final int POR_PAGINA_MAXIMO = 100;

    /** Filtro ou ordem por estrato: o estrato é calculado, então a lista é avaliada inteira. */
    public boolean dependeDaAvaliacao() {
        return (estrato != null && !estrato.isEmpty()) || ordenacao == OrdenacaoFamilia.PRIORIDADE;
    }

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
