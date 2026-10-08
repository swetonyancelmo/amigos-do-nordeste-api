package br.org.amigosdonordeste.cadastro.relatorio;

import java.util.UUID;

/** Município do filtro do mapa; codigoIbge é opcional no cadastro e pode vir null. */
public record MunicipioResponse(
        UUID id,
        String nome,
        String codigoIbge) {
}
