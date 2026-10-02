package br.org.amigosdonordeste.cadastro.precadastro;

/** Consulta de situacao com ids demais numa chamada. Vira 400. */
public class MuitosIdsException extends RuntimeException {
    public MuitosIdsException(int maximo) {
        super("ids: no máximo " + maximo + " por consulta.");
    }
}
