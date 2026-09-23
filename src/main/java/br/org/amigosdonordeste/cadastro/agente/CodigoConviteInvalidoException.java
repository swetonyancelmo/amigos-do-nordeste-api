package br.org.amigosdonordeste.cadastro.agente;

/**
 * Mensagem unica para codigo inexistente, ja usado ou de agente desativada.
 * Se a resposta fosse diferente em cada caso, virava um jeito de descobrir
 * codigos validos por tentativa.
 */
public class CodigoConviteInvalidoException extends RuntimeException {

    public CodigoConviteInvalidoException() {
        super("Código inválido.");
    }
}
