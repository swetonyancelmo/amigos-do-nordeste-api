package br.org.amigosdonordeste.cadastro.familia.request;

import br.org.amigosdonordeste.cadastro.familia.enums.AbastecimentoAgua;
import br.org.amigosdonordeste.cadastro.familia.enums.EscoamentoSanitario;
import br.org.amigosdonordeste.cadastro.familia.enums.TratamentoAgua;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.FaixaRenda;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.TipoFonteRenda;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Parentesco;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Serie;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Sexo;
import br.org.amigosdonordeste.cadastro.pessoa.enums.TamanhoRoupa;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Corpo do PUT /api/familias/{id}. Aqui o id é como o servidor sabe quem já
 * existe: com id = atualiza · sem id = cria · sumiu do array = remove.
 */
public record AtualizarFamiliaRequisicao(
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
        @NotNull @Valid List<AtualizarPessoa> pessoas,
        @NotNull @Valid List<AtualizarFonteRenda> fontesRenda,
        String observacoes
) implements CamposFamilia {

    public record AtualizarPessoa(
            // null = pessoa nova
            UUID id,
            @Size(max = 120) String nome,
            Boolean cadastroIncompleto,
            Sexo sexo,
            LocalDate dataNascimento,
            Integer idadeEstimada,
            LocalDate idadeEstimadaEm,
            Parentesco parentesco,
            Boolean estuda,
            Serie serie,
            TamanhoRoupa tamanhoRoupa,
            // validado contra NumerosCalcado.VALORES no FamiliaService
            String numeroCalcado,
            Boolean gestante,
            String observacoes
    ) implements CamposPessoa {
    }

    public record AtualizarFonteRenda(
            // null = fonte nova
            UUID id,
            @NotNull TipoFonteRenda tipo,
            // id de uma pessoa que já existe nesta família e continua em
            // pessoas[]. null = fonte da família.
            UUID pessoaId,
            FaixaRenda faixa,
            String observacao
    ) {
    }
}
