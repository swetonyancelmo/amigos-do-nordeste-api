package br.org.amigosdonordeste.cadastro.comunidade.exception;

import java.util.UUID;

public class ComunidadeNaoEncontradaException extends RuntimeException {
  public ComunidadeNaoEncontradaException(UUID id) {
    super("Comunidade com ID " + id + " não encontrada.");
  }
}
