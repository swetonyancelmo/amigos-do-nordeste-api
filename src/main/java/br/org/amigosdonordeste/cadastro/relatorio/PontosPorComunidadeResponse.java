package br.org.amigosdonordeste.cadastro.relatorio;

import java.util.List;

public record PontosPorComunidadeResponse(
  MunicipioResponse municipio,
  List<PontoComunidadeResponse> pontos
) {}
