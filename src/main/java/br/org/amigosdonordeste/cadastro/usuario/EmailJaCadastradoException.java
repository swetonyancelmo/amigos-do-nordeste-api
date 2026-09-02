package br.org.amigosdonordeste.cadastro.usuario;

public class EmailJaCadastradoException extends RuntimeException {
    public EmailJaCadastradoException() {
        super("Já existe uma conta com esse e-mail.");
    }
}
