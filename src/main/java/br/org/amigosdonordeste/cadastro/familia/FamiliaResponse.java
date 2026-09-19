package br.org.amigosdonordeste.cadastro.familia;

import br.org.amigosdonordeste.cadastro.familia.enums.AbastecimentoAgua;
import br.org.amigosdonordeste.cadastro.familia.enums.EscoamentoSanitario;
import br.org.amigosdonordeste.cadastro.familia.enums.TratamentoAgua;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record FamiliaResponse(
        UUID id,
        UUID comunidadeId,
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
        Totais totais
) {
    // calculados na hora, nunca gravados — regra do projeto
    public record Totais(int totalPessoas, int totalPessoasEstudando, int totalFontesRenda) {
    }

    public static FamiliaResponse fromEntity(Familia familia) {
        List<PessoaResponse> pessoas = familia.getPessoas().stream()
                .map(PessoaResponse::fromEntity)
                .toList();

        List<FonteRendaResponse> fontes = familia.getFontesRenda().stream()
                .map(FonteRendaResponse::fromEntity)
                .toList();

        long estudando = PessoaResponse.contarEstudando(pessoas);

        return new FamiliaResponse(
                familia.getId(),
                familia.getComunidade().getId(),
                familia.getResponsavelNome(),
                familia.getResponsavelCpf(),
                familia.getTelefone(),
                familia.getPontoReferencia(),
                familia.getTemBanheiro(),
                familia.getEscoamentoSanitario(),
                familia.getTratamentoAgua(),
                familia.getAbastecimentoAgua(),
                pessoas,
                fontes,
                familia.getObservacoes(),
                new Totais(pessoas.size(), (int) estudando, fontes.size())
        );
    }
}