package br.org.amigosdonordeste.cadastro.vulnerabilidade.motor;

/**
 * Se a sentinela do instrumento entra no escore. Toda sentinela do artigo
 * tem linha na base, mesmo as que não entram, para o corte ficar registrado
 * no dado e aparecer no relatório (ADR-0010).
 */
public enum SituacaoSentinela {
    /** Entra no escore. */
    AVALIADA,
    /** O cadastro não tem o dado (ex.: analfabetismo). */
    NAO_COLETADA,
    /** Dado de saúde: de propósito não é coletado (LGPD). */
    DESCARTADA_LGPD
}
