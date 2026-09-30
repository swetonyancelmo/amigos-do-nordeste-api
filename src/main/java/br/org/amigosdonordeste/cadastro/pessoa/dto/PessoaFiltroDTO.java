package br.org.amigosdonordeste.cadastro.pessoa.dto;

import br.org.amigosdonordeste.cadastro.pessoa.enums.FaixaEtaria;

import java.util.UUID;

/** Parâmetros de GET /api/pessoas. Todos opcionais e combináveis (AND). */
public record PessoaFiltroDTO(
    String nome,
    UUID comunidadeId,
    UUID municipioId,
    UUID familiaId,
    Boolean cadastroIncompleto,
    Boolean estuda,
    FaixaEtaria faixaEtaria,
    Integer pagina,
    Integer tamanho
) {
    public static final int TAMANHO_PADRAO = 20;
    public static final int TAMANHO_MAXIMO = 100;

    public int paginaNormalizada() {
        return (pagina == null || pagina < 0) ? 0 : pagina;
    }

    public int tamanhoNormalizado() {
        if (tamanho == null || tamanho <= 0) {
            return TAMANHO_PADRAO;
        }
        return Math.min(tamanho, TAMANHO_MAXIMO);
    }
}
