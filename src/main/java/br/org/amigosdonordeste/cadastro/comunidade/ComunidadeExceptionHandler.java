package br.org.amigosdonordeste.cadastro.comunidade;

import br.org.amigosdonordeste.cadastro.comunidade.exception.ComunidadeNaoEncontradaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.Map;

public class ComunidadeExceptionHandler {

  @ExceptionHandler(ComunidadeNaoEncontradaException.class)
  public ResponseEntity<Map<String, Object>> handlerNaoEncontrada(ComunidadeNaoEncontradaException ex){
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
      "timestamp", Instant.now().toString(),
      "status", 404,
      "message", ex.getMessage()
      ));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handlerIllegalArgumentException(IllegalArgumentException ex){
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
      "timestamp", Instant.now().toString(),
      "status", 400,
      "message", ex.getMessage()
    ));
  }
}
