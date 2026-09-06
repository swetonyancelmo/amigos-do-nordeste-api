package br.org.amigosdonordeste.cadastro.pessoa.enuns;

/**
 * Parentesco do membro com a pessoa responsavel pela familia. Lista fechada,
 * valores provisorios — confirmar com a associacao e servir por /api/metadados.
 */
public enum Parentesco {
    RESPONSAVEL,
    CONJUGE,
    FILHO,
    ENTEADO,
    PAI_OU_MAE,
    SOGRO,
    GENRO_OU_NORA,
    NETO,
    AVO,
    IRMAO,
    OUTRO_PARENTE,
    AGREGADO,
    SEM_PARENTESCO
}
