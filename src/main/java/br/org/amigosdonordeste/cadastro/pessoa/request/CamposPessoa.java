package br.org.amigosdonordeste.cadastro.pessoa.request;

import br.org.amigosdonordeste.cadastro.pessoa.enums.Parentesco;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Serie;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Sexo;
import br.org.amigosdonordeste.cadastro.pessoa.enums.TamanhoRoupa;

import java.time.LocalDate;

/**
 * Os campos de pessoa que todo caminho de escrita recebe — o payload da
 * família (POST/PUT /api/familias) e a tela de pessoa (POST
 * /api/familias/{familiaId}/pessoas, PUT /api/pessoas/{id}). Todos passam pelo
 * mesmo PessoaService.aplicarCampos: é o que impede os caminhos de divergirem.
 */
public interface CamposPessoa {
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
