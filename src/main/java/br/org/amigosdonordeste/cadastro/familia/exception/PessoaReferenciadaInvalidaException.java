package br.org.amigosdonordeste.cadastro.familia.exception;

import java.util.UUID;

public class PessoaReferenciadaInvalidaException extends RuntimeException {
    public PessoaReferenciadaInvalidaException(UUID pessoaId) {
        super("fontesRenda.pessoaId não corresponde a nenhuma pessoa informada no payload: " + pessoaId);
    }
}