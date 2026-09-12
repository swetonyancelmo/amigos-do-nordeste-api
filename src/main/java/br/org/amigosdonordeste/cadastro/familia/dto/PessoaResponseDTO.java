package br.org.amigosdonordeste.cadastro.familia.dto;

import br.org.amigosdonordeste.cadastro.pessoa.Pessoa;
import br.org.amigosdonordeste.cadastro.pessoa.enuns.Parentesco;
import br.org.amigosdonordeste.cadastro.pessoa.enuns.Sexo;
import br.org.amigosdonordeste.cadastro.pessoa.enuns.TamanhoRoupa;

import java.time.LocalDate;
import java.util.UUID;

public record PessoaResponseDTO(
        UUID id,
        String nome,
        boolean cadastroIncompleto,
        Sexo sexo,
        LocalDate dataNascimento,
        Integer idadeEstimada,
        LocalDate idadeEstimadaEm,
        Parentesco parentesco,
        Boolean estuda,
        String serie,
        TamanhoRoupa tamanhoRoupa,
        Integer numeroCalcado,
        Boolean gestante,
        String observacoes
) {
    public static PessoaResponseDTO from(Pessoa pessoa) {
        return new PessoaResponseDTO(
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.isCadastroIncompleto(),
                pessoa.getSexo(),
                pessoa.getDataNascimento(),
                pessoa.getIdadeEstimada(),
                pessoa.getIdadeEstimadaEm(),
                pessoa.getParentesco(),
                pessoa.getEstuda(),
                pessoa.getSerie(),
                pessoa.getTamanhoRoupa(),
                pessoa.getNumeroCalcado(),
                pessoa.getGestante(),
                pessoa.getObservacoes()
        );
    }
}