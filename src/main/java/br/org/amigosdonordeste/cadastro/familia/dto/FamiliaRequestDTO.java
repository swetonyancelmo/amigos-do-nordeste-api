package br.org.amigosdonordeste.cadastro.familia.dto;

import br.org.amigosdonordeste.cadastro.familia.enuns.AbastecimentoAgua;
import br.org.amigosdonordeste.cadastro.familia.enuns.EscoamentoSanitario;
import br.org.amigosdonordeste.cadastro.familia.enuns.TratamentoAgua;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record FamiliaRequestDTO(
        @NotNull
        UUID comunidadeId,

        @NotBlank
        String responsavelNome,

        String responsavelCpf,

        String telefone,

        String pontoReferencia,

        Boolean temBanheiro,

        EscoamentoSanitario escoamentoSanitario,

        TratamentoAgua tratamentoAgua,

        Set<AbastecimentoAgua> abastecimentoAgua,

        // pode vir vazia: família cadastrada antes dos membros
        @NotNull
        @Valid
        List<PessoaRequestDTO> pessoas,

        @NotNull
        @Valid
        List<FonteRendaRequestDTO> fontesRenda,

        String observacoes
) {
}