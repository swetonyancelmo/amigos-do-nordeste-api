package br.org.amigosdonordeste.cadastro.fonterenda.enuns;

/**
 * Tipo da fonte de renda. Lista fechada; a associacao citou bolsa familia,
 * aposentadoria, BPC e trabalho sazonal (ver docs/decisoes/ADR-0003). Valores
 * provisorios, a servir por /api/metadados.
 */
public enum TipoFonteRenda {
    BOLSA_FAMILIA,
    BPC,
    APOSENTADORIA,
    PENSAO,
    AUXILIO_DOENCA,
    TRABALHO_FORMAL,
    TRABALHO_INFORMAL,
    TRABALHO_SAZONAL,
    OUTRO
}
