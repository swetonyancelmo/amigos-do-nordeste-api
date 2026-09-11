package br.org.amigosdonordeste.cadastro.municipio;

import java.util.UUID;

public class MunicipioNaoEncontradoException extends RuntimeException {
  public MunicipioNaoEncontradoException(UUID id) {
    super("Município com ID " + id + " não encontrado.");
  }
}
