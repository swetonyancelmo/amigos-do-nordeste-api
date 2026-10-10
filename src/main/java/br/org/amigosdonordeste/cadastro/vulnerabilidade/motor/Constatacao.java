package br.org.amigosdonordeste.cadastro.vulnerabilidade.motor;

/**
 * O que o motor concluiu sobre uma sentinela numa família (ADR-0010, regra 1).
 *
 *   - PRESENTE: o dado existe e dispara a sentinela; soma os pontos;
 *   - AUSENTE: o dado existe e diz que não; soma zero;
 *   - INDETERMINADA: o dado não existe. Não soma e conta para decidir se a
 *     família está em DADOS_INSUFICIENTES. Nunca é tratada como AUSENTE.
 */
public enum Constatacao {
    PRESENTE,
    AUSENTE,
    INDETERMINADA
}
