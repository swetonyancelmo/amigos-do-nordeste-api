package br.org.amigosdonordeste.cadastro.familia.exception;

import java.util.UUID;

public class PessoaReferenciadaInvalidaException extends RuntimeException {

    /** PUT: pessoaId não é de uma pessoa desta família presente no payload. */
    public PessoaReferenciadaInvalidaException(UUID pessoaId) {
        super("fontesRenda.pessoaId não corresponde a nenhuma pessoa desta família informada no payload: " + pessoaId);
    }

    /** POST: pessoaIndice fora de pessoas[]. */
    public PessoaReferenciadaInvalidaException(int pessoaIndice, int totalPessoas) {
        this("fontesRenda.pessoaIndice", pessoaIndice, totalPessoas);
    }

    /** Qualquer campo que aponte para uma posição em pessoas[] (ex.: pessoas.indice na aprovação do pré-cadastro). */
    public PessoaReferenciadaInvalidaException(String campo, int indice, int totalPessoas) {
        super(campo + " " + indice + " está fora de pessoas[] (tamanho " + totalPessoas + ").");
    }
}
