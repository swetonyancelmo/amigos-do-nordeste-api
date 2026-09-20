package br.org.amigosdonordeste.cadastro.relatorio;

import java.util.List;

/**
 * Issue #18: a "lista de compras" da associação — quantas peças de roupa e
 * pares de calçado comprar por tamanho, contados na hora a partir do que já
 * está cadastrado (regra 2 do projeto: nada aqui é digitado).
 */
public record NecessidadesResponse(
        long totalFamilias,
        long totalPessoas,
        long totalCriancasAte12,
        List<ItemContagem> roupa,
        List<ItemContagem> calcado
) {


    public record ItemContagem(String chave, long quantidade) {
    }
}
