package br.org.amigosdonordeste.cadastro.relatorio;

import java.util.UUID;

public record MunicipioResponse(
  UUID id,
  String nome,
  String codigoIbge
) {}
