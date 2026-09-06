package br.org.amigosdonordeste.cadastro.familia.enuns;

/**
 * Como a familia trata a agua de beber. Categorias baseadas na Ficha de
 * Cadastro Domiciliar do e-SUS (ver docs/decisoes/ADR-0003). Valores
 * provisorios, a servir por /api/metadados.
 */
public enum TratamentoAgua {
    FILTRACAO,
    FERVURA,
    CLORACAO,
    SEM_TRATAMENTO,
    OUTRO
}
