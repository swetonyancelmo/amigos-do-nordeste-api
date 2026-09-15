package br.org.amigosdonordeste.cadastro.familia.request;

import br.org.amigosdonordeste.cadastro.fonterenda.enums.FaixaRenda;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.TipoFonteRenda;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FonteRendaRequest(
        UUID id,
        @NotNull TipoFonteRenda tipo,
        // aponta pro "id" de um item de pessoas[] neste payload, ou pro id
        // já existente no banco (no PUT). null = fonte da família.
        UUID pessoaId,
        FaixaRenda faixa,
        String observacao
) {
}