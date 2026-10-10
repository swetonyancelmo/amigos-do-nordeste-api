package br.org.amigosdonordeste.cadastro.familia;

import java.util.UUID;

import br.org.amigosdonordeste.cadastro.dominio.Idade;
import br.org.amigosdonordeste.cadastro.pessoa.Pessoa;
import br.org.amigosdonordeste.cadastro.pessoa.enums.FaixaEtaria;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.Avaliacao;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.EstratoRisco;

/**
 * Uma linha de GET /api/familias (issue #16) — também a resposta de
 * inativar/reativar (issue #43).
 *
 * Os totais são contados na hora a partir de pessoas (regra 2: nunca coluna).
 * Na listagem, pessoas já vem no join fetch de buscarComPessoasPorIds; as três
 * faixas mais totalSemIdadeConhecida somam totalPessoas, como na ficha.
 *
 * vulnerabilidade é o resumo da avaliação (ADR-0010); a explicação inteira
 * fica na ficha (GET /api/familias/{id}).
 */
public record FamiliaResumoResponse(
        UUID id,
        String responsavelNome,
        UUID comunidadeId,
        String comunidadeNome,
        String municipioNome,
        boolean ativa,
        boolean semBanheiro,
        int totalPessoas,
        int totalAte12Anos,
        int totalDe13A59Anos,
        int total60AnosOuMais,
        int totalSemIdadeConhecida,
        Vulnerabilidade vulnerabilidade
) {
    /** escore null em DADOS_INSUFICIENTES; pontosConfirmados é o que já se sabe. */
    public record Vulnerabilidade(EstratoRisco estrato, String rotulo, Integer escore, int pontosConfirmados) {
        static Vulnerabilidade de(Avaliacao avaliacao) {
            return new Vulnerabilidade(avaliacao.estrato(), avaliacao.rotulo(), avaliacao.escore(),
                    avaliacao.pontosConfirmados());
        }
    }

    public static FamiliaResumoResponse fromEntity(Familia familia, Avaliacao avaliacao) {
        int ate12 = 0;
        int de13a59 = 0;
        int de60ouMais = 0;
        int semIdade = 0;

        for (Pessoa pessoa : familia.getPessoas()) {
            FaixaEtaria faixa = FaixaEtaria.de(Idade.calcular(
                    pessoa.getDataNascimento(),
                    pessoa.getIdadeEstimada(),
                    pessoa.getIdadeEstimadaEm()));
            if (faixa == null) {
                semIdade++;
                continue;
            }
            switch (faixa) {
                case ATE_12 -> ate12++;
                case DE_13_A_59 -> de13a59++;
                case DE_60_OU_MAIS -> de60ouMais++;
                default -> throw new IllegalStateException("Faixa etária sem contagem: " + faixa);
            }
        }

        return new FamiliaResumoResponse(
                familia.getId(),
                familia.getResponsavelNome(),
                familia.getComunidade().getId(),
                familia.getComunidade().getNome(),
                familia.getComunidade().getMunicipio().getNome(),
                familia.isAtiva(),
                // mesmo critério de contarSemBanheiro: não informado conta
                !Boolean.TRUE.equals(familia.getTemBanheiro()),
                familia.getPessoas().size(),
                ate12,
                de13a59,
                de60ouMais,
                semIdade,
                Vulnerabilidade.de(avaliacao));
    }
}
