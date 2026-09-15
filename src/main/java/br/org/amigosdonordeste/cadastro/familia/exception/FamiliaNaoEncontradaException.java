package br.org.amigosdonordeste.cadastro.familia.exception;

import java.util.UUID;

public class FamiliaNaoEncontradaException extends RuntimeException {
    public FamiliaNaoEncontradaException(UUID id) {
        super("Família com ID " + id + " não encontrada.");
    }
}