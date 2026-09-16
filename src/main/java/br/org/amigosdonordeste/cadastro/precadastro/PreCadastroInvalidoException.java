package br.org.amigosdonordeste.cadastro.precadastro;

/** Regra de conteudo do pre-cadastro que o Bean Validation sozinho nao expressa. Vira 400. */
public class PreCadastroInvalidoException extends RuntimeException {
    public PreCadastroInvalidoException(String mensagem) {
        super(mensagem);
    }
}
