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
        List<ItemContagem> calcado,
        // Quem entrou na contagem mas não tem tamanho/calçado preenchido
        // (ex.: família vinda do app da ACS). Nunca é somado a uma faixa, e
        // sai no JSON mesmo quando é zero — para ninguém comprar a menos sem ver.
        long semTamanhoInformado,
        long semCalcadoInformado,
        // Pessoas no escopo sem data de nascimento nem idade estimada. Com
        // todasIdades=false elas não entram na roupa/calçado, porque não dá
        // para saber se têm até 12 anos.
        long semIdadeInformada
) {

    public record ItemContagem(String chave, long quantidade) {
    }
}
