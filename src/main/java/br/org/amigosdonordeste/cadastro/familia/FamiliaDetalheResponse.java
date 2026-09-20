package br.org.amigosdonordeste.cadastro.familia;

import br.org.amigosdonordeste.cadastro.comunidade.ComunidadeResponse;
import br.org.amigosdonordeste.cadastro.familia.enums.AbastecimentoAgua;
import br.org.amigosdonordeste.cadastro.familia.enums.EscoamentoSanitario;
import br.org.amigosdonordeste.cadastro.familia.enums.TratamentoAgua;
import br.org.amigosdonordeste.cadastro.pessoa.enums.FaixaEtaria;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A ficha inteira da família — é o que a tela de edição carrega (issue #17).
 *
 * Diferente do {@link FamiliaResponse} do POST/PUT, que devolve só o
 * comunidadeId: aqui a comunidade vem aninhada com o município, porque a tela
 * mostra "Sítio Alegre — Ibimi/PE" no cabeçalho e não deve precisar de uma
 * segunda chamada só para isso.
 */
public record FamiliaDetalheResponse(
        UUID id,
        ComunidadeResponse comunidade,
        String responsavelNome,
        String responsavelCpf,
        String telefone,
        String pontoReferencia,
        Boolean temBanheiro,
        EscoamentoSanitario escoamentoSanitario,
        TratamentoAgua tratamentoAgua,
        Set<AbastecimentoAgua> abastecimentoAgua,
        List<PessoaResponse> pessoas,
        List<FonteRendaResponse> fontesRenda,
        String observacoes,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm,
        Totais totais
) {

    /**
     * Contados na hora, a partir de pessoas[] e fontesRenda[] — nenhum deles é
     * coluna no banco (regra 2 do projeto). As três faixas mais
     * totalSemIdadeConhecida somam totalPessoas: quem não tem data de
     * nascimento nem idade estimada não entra em faixa nenhuma.
     */
    public record Totais(
            int totalPessoas,
            int totalAte12Anos,
            int totalDe13A59Anos,
            int total60AnosOuMais,
            int totalSemIdadeConhecida,
            int totalPessoasEstudando,
            int totalFontesRenda
    ) {
    }

    public static FamiliaDetalheResponse fromEntity(Familia familia) {
        List<PessoaResponse> pessoas = familia.getPessoas().stream()
                .map(PessoaResponse::fromEntity)
                .toList();

        List<FonteRendaResponse> fontes = familia.getFontesRenda().stream()
                .map(FonteRendaResponse::fromEntity)
                .toList();

        // copia, nao a colecao da entidade: abastecimentoAgua e lazy e so o
        // Jackson a leria — ja fora da transacao do service, o que estoura
        // LazyInitializationException na serializacao (500). Copiar aqui
        // inicializa dentro da transacao e mantem a regra 9 (a resposta nao
        // carrega nada da entidade).
        Set<AbastecimentoAgua> abastecimento = new LinkedHashSet<>(familia.getAbastecimentoAgua());

        return new FamiliaDetalheResponse(
                familia.getId(),
                ComunidadeResponse.fromEntity(familia.getComunidade()),
                familia.getResponsavelNome(),
                familia.getResponsavelCpf(),
                familia.getTelefone(),
                familia.getPontoReferencia(),
                familia.getTemBanheiro(),
                familia.getEscoamentoSanitario(),
                familia.getTratamentoAgua(),
                abastecimento,
                pessoas,
                fontes,
                familia.getObservacoes(),
                familia.getCriadoEm(),
                familia.getAtualizadoEm(),
                contar(pessoas, fontes)
        );
    }

    private static Totais contar(List<PessoaResponse> pessoas, List<FonteRendaResponse> fontes) {
        int ate12 = 0;
        int de13a59 = 0;
        int de60ouMais = 0;
        int semIdade = 0;

        for (PessoaResponse pessoa : pessoas) {
            FaixaEtaria faixa = FaixaEtaria.de(pessoa.idade());
            if (faixa == null) {
                semIdade++;
                continue;
            }
            switch (faixa) {
                case ATE_12 -> ate12++;
                case DE_13_A_59 -> de13a59++;
                case DE_60_OU_MAIS -> de60ouMais++;
                // se uma faixa nova for adicionada ao enum, falha alto aqui em
                // vez de subcontar em silencio nos totais
                default -> throw new IllegalStateException("Faixa etária sem contagem: " + faixa);
            }
        }

        long estudando = PessoaResponse.contarEstudando(pessoas);

        return new Totais(pessoas.size(), ate12, de13a59, de60ouMais, semIdade, (int) estudando, fontes.size());
    }
}
