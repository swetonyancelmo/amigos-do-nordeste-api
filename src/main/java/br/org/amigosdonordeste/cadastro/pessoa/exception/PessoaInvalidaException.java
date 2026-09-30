package br.org.amigosdonordeste.cadastro.pessoa.exception;

/**
 * Regra de pessoa quebrada na tela de pessoa (nome, idade). A mensagem vai
 * direto para a usuária — nunca coloque nela o nome ou outro dado da pessoa.
 */
public class PessoaInvalidaException extends RuntimeException {
    public PessoaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
