package br.org.amigosdonordeste.cadastro.relatorio;

import java.math.BigDecimal;
import java.util.UUID;

public record PontoComunidadeResponse(
  UUID id,
  String nome,
  BigDecimal latitude,
  BigDecimal longitude,
  Long totalFamilias
) {}
