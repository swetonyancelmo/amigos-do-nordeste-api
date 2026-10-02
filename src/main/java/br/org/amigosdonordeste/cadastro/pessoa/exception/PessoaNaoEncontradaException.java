package br.org.amigosdonordeste.cadastro.pessoa.exception;

import java.util.UUID;

public class PessoaNaoEncontradaException extends RuntimeException {
    public PessoaNaoEncontradaException(UUID id) {
        super("Pessoa com ID " + id + " não encontrada.");
    }
}
