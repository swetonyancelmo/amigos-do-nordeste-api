package br.org.amigosdonordeste.cadastro.familia.exception;

public class NumeroCalcadoInvalidoException extends RuntimeException {
    public NumeroCalcadoInvalidoException(String valor) {
        super("numeroCalcado inválido: \"" + valor + "\". Veja os valores aceitos em NumerosCalcado.VALORES.");
    }
}