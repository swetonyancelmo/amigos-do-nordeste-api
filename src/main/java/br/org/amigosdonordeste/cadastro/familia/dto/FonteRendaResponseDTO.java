package br.org.amigosdonordeste.cadastro.familia.dto;

import br.org.amigosdonordeste.cadastro.fonterenda.FonteRenda;
import br.org.amigosdonordeste.cadastro.fonterenda.enuns.FaixaRenda;
import br.org.amigosdonordeste.cadastro.fonterenda.enuns.TipoFonteRenda;

import java.util.UUID;

public record FonteRendaResponseDTO(
        UUID id,
        TipoFonteRenda tipo,
        UUID pessoaId,
        FaixaRenda faixa,
        String observacao
) {
    public static FonteRendaResponseDTO from(FonteRenda fonteRenda) {
        return new FonteRendaResponseDTO(
                fonteRenda.getId(),
                fonteRenda.getTipo(),
                fonteRenda.getPessoa() != null ? fonteRenda.getPessoa().getId() : null,
                fonteRenda.getFaixa(),
                fonteRenda.getObservacao()
        );
    }
}