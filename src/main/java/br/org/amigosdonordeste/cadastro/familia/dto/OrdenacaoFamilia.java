package br.org.amigosdonordeste.cadastro.familia.dto;

/**
 * Ordem de GET /api/familias. NOME é o padrão de sempre. PRIORIDADE segue a
 * ordem dos estratos na base de conhecimento (ADR-0010) e, dentro do
 * estrato, os pontos confirmados: é uma sugestão de leitura, não uma fila
 * de atendimento.
 */
public enum OrdenacaoFamilia {
    NOME,
    PRIORIDADE
}
