package br.org.amigosdonordeste.cadastro.familia.dto;

import br.org.amigosdonordeste.cadastro.familia.Familia;
import br.org.amigosdonordeste.cadastro.familia.enuns.AbastecimentoAgua;
import br.org.amigosdonordeste.cadastro.familia.enuns.EscoamentoSanitario;
import br.org.amigosdonordeste.cadastro.familia.enuns.TratamentoAgua;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record FamiliaResponseDTO(
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
        List<PessoaResponseDTO> pessoas,
        List<FonteRendaResponseDTO> fontesRenda,
        String observacoes,
        TotaisFamiliaDTO totais
) {

    // Totais calculados na hora, nunca gravados no banco — regra do
    // projeto: "nada que possa ser calculado é digitado".
    public record TotaisFamiliaDTO(
            int totalPessoas,
            int totalPessoasEstudando,
            int totalFontesRenda
    ) {
    }

    public static FamiliaResponseDTO from(Familia familia) {
        List<PessoaResponseDTO> pessoas = familia.getPessoas().stream()
                .map(PessoaResponseDTO::from)
                .toList();

        List<FonteRendaResponseDTO> fontes = familia.getFontesRenda().stream()
                .map(FonteRendaResponseDTO::from)
                .toList();

        long estudando = pessoas.stream()
                .filter(p -> Boolean.TRUE.equals(p.estuda()))
                .count();

        TotaisFamiliaDTO totais = new TotaisFamiliaDTO(
                pessoas.size(),
                (int) estudando,
                fontes.size()
        );

        return new FamiliaResponseDTO(
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
                totais
        );
    }
}