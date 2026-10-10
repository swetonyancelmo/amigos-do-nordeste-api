package br.org.amigosdonordeste.cadastro.vulnerabilidade.motor;

/**
 * A base de conhecimento no banco não fecha (estrato faltando, parâmetro que
 * não é valor de enum, sentinela avaliada sem avaliador...). Falha alto, com
 * a causa na mensagem: classificar com uma base quebrada seria pior do que
 * não classificar.
 */
public class BaseDeConhecimentoInvalidaException extends IllegalStateException {

    public BaseDeConhecimentoInvalidaException(String motivo) {
        super("Base de conhecimento de vulnerabilidade inválida: " + motivo);
    }
}
