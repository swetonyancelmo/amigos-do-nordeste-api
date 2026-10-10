package br.org.amigosdonordeste.cadastro.vulnerabilidade.motor;

/**
 * Os estratos da avaliação (ADR-0010). R1, R2 e R3 são os códigos do Quadro 02
 * da Escala de Coelho-Savassi, mantidos fiéis para o resultado poder ser
 * citado. Os outros dois não são do instrumento:
 *
 *   - SEM_RISCO_IDENTIFICADO: escore abaixo de 5, que o artigo não classifica;
 *   - DADOS_INSUFICIENTES: falta dado para decidir o estrato. Pede ação
 *     (completar o cadastro), nunca é prioridade baixa.
 *
 * O texto que aparece na tela NÃO é o nome do enum: é o rótulo configurado em
 * vulnerabilidade_estrato, servido em /api/metadados. Os cortes também moram
 * lá, nunca aqui.
 */
public enum EstratoRisco {
    R3,
    R2,
    R1,
    SEM_RISCO_IDENTIFICADO,
    DADOS_INSUFICIENTES
}
