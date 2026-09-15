package br.org.amigosdonordeste.cadastro.familia.request;

import br.org.amigosdonordeste.cadastro.familia.enums.AbastecimentoAgua;
import br.org.amigosdonordeste.cadastro.familia.enums.EscoamentoSanitario;
import br.org.amigosdonordeste.cadastro.familia.enums.TratamentoAgua;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record FamiliaRequest(
        @NotNull UUID comunidadeId,
        @NotBlank @Size(max = 120) String responsavelNome,
        // aceita com ou sem máscara; o service guarda só os 11 dígitos
        @Pattern(regexp = "^$|^\\d{11}$|^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$",
                message = "deve ter 11 dígitos, com ou sem pontuação")
        String responsavelCpf,
        @Size(max = 20) String telefone,
        @Size(max = 255) String pontoReferencia,
        Boolean temBanheiro,
        EscoamentoSanitario escoamentoSanitario,
        TratamentoAgua tratamentoAgua,
        Set<AbastecimentoAgua> abastecimentoAgua,
        // pode vir vazia: família cadastrada antes dos membros
        @NotNull @Valid List<PessoaRequest> pessoas,
        @NotNull @Valid List<FonteRendaRequest> fontesRenda,
        String observacoes
) {
}