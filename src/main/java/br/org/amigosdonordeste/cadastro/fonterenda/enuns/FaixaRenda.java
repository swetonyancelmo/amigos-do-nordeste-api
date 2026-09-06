package br.org.amigosdonordeste.cadastro.fonterenda.enuns;

/**
 * Faixa de renda em salarios minimos, NUNCA valor em reais: renda declarada por
 * um vizinho, no papel, sobre trabalho sazonal, e o dado menos confiavel do
 * cadastro — e o mais sensivel (ver docs/decisoes/ADR-0003).
 *
 * Lista fechada, valores provisorios — a servir por /api/metadados.
 */
public enum FaixaRenda {
    SEM_RENDA,
    ATE_MEIO_SM,
    DE_MEIO_A_UM_SM,
    DE_UM_A_DOIS_SM,
    ACIMA_DE_DOIS_SM
}
