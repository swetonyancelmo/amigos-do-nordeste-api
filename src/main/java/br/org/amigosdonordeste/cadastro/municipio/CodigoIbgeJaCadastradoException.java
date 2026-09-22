package br.org.amigosdonordeste.cadastro.municipio;

public class CodigoIbgeJaCadastradoException extends RuntimeException {
    public CodigoIbgeJaCadastradoException() {
        super("Já existe um município com esse código do IBGE.");
    }
}


