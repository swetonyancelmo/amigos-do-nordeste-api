package br.org.amigosdonordeste.cadastro.agente;

import java.util.UUID;

/** Vira 404. */
public class AgenteNaoEncontradoException extends RuntimeException {
    public AgenteNaoEncontradoException(UUID id) {
        super("Agente com ID " + id + " não encontrada.");
    }
}
