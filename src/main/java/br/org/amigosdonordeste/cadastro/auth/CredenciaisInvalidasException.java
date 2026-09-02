package br.org.amigosdonordeste.cadastro.auth;

public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("E-mail ou senha incorretos.");
    }

    public CredenciaisInvalidasException(String mensagem) {
        super(mensagem);
    }
}
