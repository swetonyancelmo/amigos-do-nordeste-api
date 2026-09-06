package br.org.amigosdonordeste.cadastro.familia.enuns;

/**
 * Destino do esgoto do domicilio. Categorias baseadas na Ficha de Cadastro
 * Domiciliar do e-SUS — nao inventamos lista (ver docs/decisoes/ADR-0003).
 * Valores provisorios, a servir por /api/metadados.
 */
public enum EscoamentoSanitario {
    REDE_COLETORA,
    FOSSA_SEPTICA,
    FOSSA_RUDIMENTAR,
    VALA_A_CEU_ABERTO,
    DIRETO_PARA_CORPO_DAGUA,
    OUTRO
}
