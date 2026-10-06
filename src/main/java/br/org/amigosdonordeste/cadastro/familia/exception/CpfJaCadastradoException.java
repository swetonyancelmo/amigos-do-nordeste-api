package br.org.amigosdonordeste.cadastro.familia.exception;

/**
 * O CPF já é de outra família. Diz de qual, para a usuária achar o cadastro
 * que já existe; se for de uma inativa, o caminho é reativar, não cadastrar
 * de novo. A mensagem só chega ao painel (rota ADMIN), nunca a log.
 */
public class CpfJaCadastradoException extends RuntimeException {
    public CpfJaCadastradoException(String responsavelNome, boolean ativa) {
        super("Este CPF já está cadastrado na família de " + responsavelNome
            + (ativa ? "." : " (inativa). Reative essa família em vez de cadastrar de novo."));
    }
}
