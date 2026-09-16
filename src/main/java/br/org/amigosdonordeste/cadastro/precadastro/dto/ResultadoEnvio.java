package br.org.amigosdonordeste.cadastro.precadastro.dto;

/**
 * O que o servidor responde ao aparelho. Os dois sao sucesso para o app:
 * JA_RECEBIDO e o reenvio (toque duplo, internet que caiu depois de gravar).
 */
public enum ResultadoEnvio {
    ACEITO,
    JA_RECEBIDO
}
