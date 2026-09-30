package br.org.amigosdonordeste.cadastro.pessoa;

import br.org.amigosdonordeste.cadastro.comunidade.Comunidade;
import br.org.amigosdonordeste.cadastro.dominio.Idade;
import br.org.amigosdonordeste.cadastro.familia.Familia;
import br.org.amigosdonordeste.cadastro.pessoa.dto.Vinculos.ComunidadeResumo;
import br.org.amigosdonordeste.cadastro.pessoa.dto.Vinculos.FamiliaResumo;
import br.org.amigosdonordeste.cadastro.pessoa.dto.Vinculos.MunicipioResumo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

/** Uma linha de GET /api/pessoas. */
public record PessoaResumoResponse(
        UUID id,
        @Schema(example = "Maria Exemplo") String nome,
        boolean cadastroIncompleto,
        @Schema(description = "Calculada na hora (nunca gravada). null quando não há data nem estimativa.", example = "12")
        Integer idade,
        @Schema(description = "true: a idade veio de estimativa, não de data de nascimento — a tela escreve \"11 anos (estimada)\"")
        boolean idadeEstimada,
        LocalDate dataNascimento,
        FamiliaResumo familia,
        ComunidadeResumo comunidade,
        MunicipioResumo municipio,
        Boolean estuda
) {
    public static PessoaResumoResponse fromEntity(Pessoa pessoa) {
        Integer idade = Idade.calcular(
                pessoa.getDataNascimento(),
                pessoa.getIdadeEstimada(),
                pessoa.getIdadeEstimadaEm());
        // Idade.calcular usa a data de nascimento sempre que ela existe; sem
        // ela, qualquer idade que tenha saído veio da estimativa
        boolean veioDeEstimativa = idade != null && pessoa.getDataNascimento() == null;

        Familia familia = pessoa.getFamilia();
        Comunidade comunidade = familia.getComunidade();
        return new PessoaResumoResponse(
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.isCadastroIncompleto(),
                idade,
                veioDeEstimativa,
                pessoa.getDataNascimento(),
                FamiliaResumo.de(familia),
                ComunidadeResumo.de(comunidade),
                MunicipioResumo.de(comunidade.getMunicipio()),
                pessoa.getEstuda());
    }
}
