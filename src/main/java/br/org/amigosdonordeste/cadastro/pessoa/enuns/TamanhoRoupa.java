package br.org.amigosdonordeste.cadastro.pessoa.enuns;

/**
 * Tamanho de roupa do membro. Alimenta a contagem de roupa por comunidade
 * (RF-03) — e contagem, nunca coluna de total.
 *
 * Lista fechada, valores provisorios — a associacao usa faixas propria nos
 * documentos atuais; confirmar e servir por /api/metadados.
 */
public enum TamanhoRoupa {
    RN,
    BEBE_P,
    BEBE_M,
    BEBE_G,
    INFANTIL_2,
    INFANTIL_4,
    INFANTIL_6,
    INFANTIL_8,
    INFANTIL_10,
    INFANTIL_12,
    INFANTIL_14,
    ADULTO_PP,
    ADULTO_P,
    ADULTO_M,
    ADULTO_G,
    ADULTO_GG,
    ADULTO_XG,
    ADULTO_XGG
}
