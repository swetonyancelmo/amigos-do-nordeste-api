package br.org.amigosdonordeste.cadastro.familia.exception;

import java.util.UUID;

public class IdDuplicadoNoPayloadException extends RuntimeException {
    public IdDuplicadoNoPayloadException(String lista, UUID id) {
        super(lista + ": id repetido no payload: " + id);
    }
}
