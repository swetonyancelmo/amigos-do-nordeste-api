package br.org.amigosdonordeste.cadastro.familia.dto;

import br.org.amigosdonordeste.cadastro.fonterenda.enuns.FaixaRenda;
import br.org.amigosdonordeste.cadastro.fonterenda.enuns.TipoFonteRenda;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FonteRendaRequestDTO(
        UUID id,

        @NotNull
        TipoFonteRenda tipo,

        // Aponta pro "id" de um item de pessoas[] neste mesmo payload, ou
        // pro id já existente no banco (no PUT). null = fonte da família,
        // sem pessoa específica (ex.: Bolsa Família da casa).
        UUID pessoaId,

        FaixaRenda faixa,

        String observacao
) {
}