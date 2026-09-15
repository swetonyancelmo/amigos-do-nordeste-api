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
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Corpo do POST /api/familias. Nenhum item traz id: quem gera é o servidor.
 * Só quem está offline (o app da ACS, em /api/pre-cadastros) manda id — e lá
 * é chave de idempotência, não chave de pessoa.
 */
public record CriarFamiliaRequisicao(
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
        @NotNull @Valid List<CriarPessoa> pessoas,
        @NotNull @Valid List<CriarFonteRenda> fontesRenda,
        String observacoes
) implements CamposFamilia {

    public record CriarPessoa(
            // pode vir em branco (RF-09: cadastro incompleto, ex. "filha de Jane" sem nome)
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

    public record CriarFonteRenda(
            @NotNull TipoFonteRenda tipo,
            // posição da pessoa em pessoas[] deste payload (ela ainda não tem
            // id). null = fonte da família, ex. Bolsa Família (ADR-0003).
            @PositiveOrZero Integer pessoaIndice,
            FaixaRenda faixa,
            String observacao
    ) {
    }
}
