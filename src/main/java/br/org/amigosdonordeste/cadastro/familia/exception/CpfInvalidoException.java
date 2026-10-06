package br.org.amigosdonordeste.cadastro.familia.exception;

public class CpfInvalidoException extends RuntimeException {
    public CpfInvalidoException() {
        super("responsavelCpf: CPF inválido. Confira os números.");
    }
}
