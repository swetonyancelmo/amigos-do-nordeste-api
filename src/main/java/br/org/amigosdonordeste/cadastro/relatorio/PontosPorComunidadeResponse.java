package br.org.amigosdonordeste.cadastro.relatorio;

import java.util.List;

/** municipio vem null na visão geral (sem filtro). */
public record PontosPorComunidadeResponse(
        MunicipioResponse municipio,
        List<PontoComunidadeResponse> pontos) {
}
