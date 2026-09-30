package br.org.amigosdonordeste.cadastro.pessoa.request;

import br.org.amigosdonordeste.cadastro.pessoa.enums.Parentesco;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Serie;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Sexo;
import br.org.amigosdonordeste.cadastro.pessoa.enums.TamanhoRoupa;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Corpo de POST /api/familias/{familiaId}/pessoas e PUT /api/pessoas/{id}.
 *
 * Sem comunidadeId de propósito: comunidade e município vêm da família.
 * Mudar a comunidade de alguém é mudar a família, não a pessoa.
 *
 * As regras de idade e de nome ficam no PessoaService (modo TELA_DE_PESSOA),
 * não aqui — são as mesmas que o payload da família usa.
 */
@Schema(description = "Dados de uma pessoa. Idade: data de nascimento OU idade estimada com a data da estimativa — ou nenhum dos dois.")
public record PessoaRequisicao(
        @Schema(description = "Pode vir vazio se cadastroIncompleto for true", example = "Maria Exemplo")
        @Size(max = 120) String nome,

        @Schema(description = "true permite salvar sem nome (ex.: \"filha de Fulana\"). "
                + "O servidor também marca true quando falta nome ou qualquer informação de idade.",
                example = "false")
        Boolean cadastroIncompleto,

        @Schema(example = "FEMININO")
        Sexo sexo,

        @Schema(description = "Não pode ser no futuro nem vir junto com idadeEstimada", example = "2014-05-11")
        LocalDate dataNascimento,

        @Schema(description = "Só sem dataNascimento, e sempre com idadeEstimadaEm")
        @PositiveOrZero Integer idadeEstimada,

        @Schema(description = "Quando a idade foi estimada — é o que deixa o sistema envelhecer a estimativa")
        LocalDate idadeEstimadaEm,

        @Schema(example = "FILHO")
        Parentesco parentesco,

        @Schema(example = "true")
        Boolean estuda,

        @Schema(example = "ANO_7")
        Serie serie,

        @Schema(example = "ADULTO_M")
        TamanhoRoupa tamanhoRoupa,

        @Schema(description = "Um dos valores de NumerosCalcado (ver /api/metadados)", example = "38/39")
        String numeroCalcado,

        @Schema(example = "false")
        Boolean gestante,

        String observacoes
) implements CamposPessoa {
}
