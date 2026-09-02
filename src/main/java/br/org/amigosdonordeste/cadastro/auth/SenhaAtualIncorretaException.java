package br.org.amigosdonordeste.cadastro.auth;

public class SenhaAtualIncorretaException extends RuntimeException {
    public SenhaAtualIncorretaException() {
        super("A senha atual está incorreta.");
    }
}
