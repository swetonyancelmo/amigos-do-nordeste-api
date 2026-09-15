package br.org.amigosdonordeste.cadastro.comum;

public class MuitasTentativasException extends RuntimeException {

    public MuitasTentativasException() {
        super("Muitas tentativas. Aguarde um pouco e tente de novo.");
    }
}
