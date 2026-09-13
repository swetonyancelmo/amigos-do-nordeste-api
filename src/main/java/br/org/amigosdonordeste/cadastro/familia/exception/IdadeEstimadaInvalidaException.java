package br.org.amigosdonordeste.cadastro.familia.exception;

public class IdadeEstimadaInvalidaException extends RuntimeException {
    public IdadeEstimadaInvalidaException() {
        super("idadeEstimada e idadeEstimadaEm têm que vir os dois preenchidos ou os dois em branco, "
                + "a não ser que dataNascimento seja informada.");
    }
}