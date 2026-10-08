package br.org.amigosdonordeste.cadastro.relatorio;

import java.math.BigDecimal;
import java.util.UUID;

/** Um ponto do mapa: uma comunidade (nunca uma família) e quantas famílias ativas ela tem. */
public record PontoComunidadeResponse(
        UUID comunidadeId,
        String nome,
        BigDecimal latitude,
        BigDecimal longitude,
        Long familias) {
}
