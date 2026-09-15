package br.org.amigosdonordeste.cadastro.familia.request;

import br.org.amigosdonordeste.cadastro.pessoa.enums.Parentesco;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Serie;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Sexo;
import br.org.amigosdonordeste.cadastro.pessoa.enums.TamanhoRoupa;

import java.time.LocalDate;

/** O que POST e PUT têm em comum na pessoa — tudo menos o id. */
public sealed interface CamposPessoa
        permits CriarFamiliaRequisicao.CriarPessoa, AtualizarFamiliaRequisicao.AtualizarPessoa {
    String nome();
    Boolean cadastroIncompleto();
    Sexo sexo();
    LocalDate dataNascimento();
    Integer idadeEstimada();
    LocalDate idadeEstimadaEm();
    Parentesco parentesco();
    Boolean estuda();
    Serie serie();
    TamanhoRoupa tamanhoRoupa();
    String numeroCalcado();
    Boolean gestante();
    String observacoes();
}
