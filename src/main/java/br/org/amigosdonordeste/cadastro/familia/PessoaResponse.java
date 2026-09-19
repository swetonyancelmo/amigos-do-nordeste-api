package br.org.amigosdonordeste.cadastro.familia;

import br.org.amigosdonordeste.cadastro.dominio.Idade;
import br.org.amigosdonordeste.cadastro.pessoa.Pessoa;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Parentesco;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Serie;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Sexo;
import br.org.amigosdonordeste.cadastro.pessoa.enums.TamanhoRoupa;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PessoaResponse(
        UUID id,
        String nome,
        boolean cadastroIncompleto,
        Sexo sexo,
        LocalDate dataNascimento,
        Integer idadeEstimada,
        LocalDate idadeEstimadaEm,
        // calculada na hora com dominio.Idade — nunca gravada no banco
        Integer idade,
        Parentesco parentesco,
        Boolean estuda,
        Serie serie,
        TamanhoRoupa tamanhoRoupa,
        String numeroCalcado,
        Boolean gestante,
        String observacoes
) {
    public static PessoaResponse fromEntity(Pessoa pessoa) {
        Integer idade = Idade.calcular(
                pessoa.getDataNascimento(),
                pessoa.getIdadeEstimada(),
                pessoa.getIdadeEstimadaEm());

        return new PessoaResponse(
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.isCadastroIncompleto(),
                pessoa.getSexo(),
                pessoa.getDataNascimento(),
                pessoa.getIdadeEstimada(),
                pessoa.getIdadeEstimadaEm(),
                idade,
                pessoa.getParentesco(),
                pessoa.getEstuda(),
                pessoa.getSerie(),
                pessoa.getTamanhoRoupa(),
                pessoa.getNumeroCalcado(),
                pessoa.getGestante(),
                pessoa.getObservacoes()
        );
    }

    /** Regra de "quantos estudam" — usada tanto no POST/PUT quanto na ficha completa. */
    public static long contarEstudando(List<PessoaResponse> pessoas) {
        return pessoas.stream().filter(p -> Boolean.TRUE.equals(p.estuda())).count();
    }
}