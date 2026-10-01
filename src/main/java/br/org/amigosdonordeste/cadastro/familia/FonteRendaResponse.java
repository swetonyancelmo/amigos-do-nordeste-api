package br.org.amigosdonordeste.cadastro.familia;

import br.org.amigosdonordeste.cadastro.fonterenda.FonteRenda;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.TipoFonteRenda;

import java.util.UUID;

public record FonteRendaResponse(
        UUID id,
        TipoFonteRenda tipo,
        UUID pessoaId,
        String observacao
) {
    public static FonteRendaResponse fromEntity(FonteRenda fonteRenda) {
        return new FonteRendaResponse(
                fonteRenda.getId(),
                fonteRenda.getTipo(),
                fonteRenda.getPessoa() != null ? fonteRenda.getPessoa().getId() : null,
                fonteRenda.getObservacao()
        );
    }
}