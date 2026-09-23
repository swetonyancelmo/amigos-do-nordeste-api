package br.org.amigosdonordeste.cadastro.precadastro;

import java.util.UUID;

/** Vira 404. */
public class PreCadastroNaoEncontradoException extends RuntimeException {
    public PreCadastroNaoEncontradoException(UUID id) {
        super("Pré-cadastro com ID " + id + " não encontrado.");
    }
}
