package br.org.amigosdonordeste.cadastro.precadastro.dto;

import br.org.amigosdonordeste.cadastro.familia.enums.AbastecimentoAgua;
import br.org.amigosdonordeste.cadastro.familia.enums.EscoamentoSanitario;
import br.org.amigosdonordeste.cadastro.familia.enums.TratamentoAgua;
import br.org.amigosdonordeste.cadastro.familia.request.CriarFamiliaRequisicao.CriarFonteRenda;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Parentesco;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Serie;
import br.org.amigosdonordeste.cadastro.pessoa.enums.TamanhoRoupa;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Corpo do POST /api/pre-cadastros/{id}/aprovar: o que a agente NAO coleta.
 * O formulario do app e curto de proposito; e na revisao que o cadastro fica
 * completo. Tudo aqui e opcional — aprovar com {} tem que funcionar. O que nao
 * pode e nao existir onde preencher: sem tamanhoRoupa e numeroCalcado a
 * familia some do relatorio de necessidades.
 *
 * Nome, telefone, ponto de referencia e as pessoas (nome, sexo, idade) vem do
 * payload do pre-cadastro e nao se repetem aqui.
 */
public record AprovarPreCadastroRequisicao(
    @Schema(description = "Só é preciso quando o servidor não reconheceu a comunidade que o aparelho mandou. "
        + "Se vier, prevalece sobre a do pré-cadastro.")
    UUID comunidadeId,

    @Pattern(regexp = "^$|^\\d{11}$|^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$",
        message = "deve ter 11 dígitos, com ou sem pontuação")
    String responsavelCpf,

    Boolean temBanheiro,
    EscoamentoSanitario escoamentoSanitario,
    TratamentoAgua tratamentoAgua,
    Set<AbastecimentoAgua> abastecimentoAgua,

    @Schema(description = "pessoaIndice é a posição na lista de pessoas do payload original, como no POST /api/familias")
    @Valid List<@NotNull CriarFonteRenda> fontesRenda,

    @Schema(description = "Complementos por pessoa, referenciadas pela posição no payload original")
    @Valid List<@NotNull ComplementoPessoa> pessoas,

    String observacoes
) {

    /** O que a agente nao pergunta sobre cada pessoa. indice e posicao, nao id: a pessoa so ganha id na aprovacao. */
    public record ComplementoPessoa(
        @Schema(description = "Posição da pessoa na lista de pessoas do payload original (começa em 0)")
        @NotNull @PositiveOrZero Integer indice,
        Parentesco parentesco,
        Boolean estuda,
        Serie serie,
        TamanhoRoupa tamanhoRoupa,
        @Schema(description = "Validado contra a lista de GET /api/metadados", example = "30/31")
        String numeroCalcado,
        Boolean gestante
    ) { }

    /** Corpo ausente ou {} — aprovar sem completar nada. */
    public static AprovarPreCadastroRequisicao vazia() {
        return new AprovarPreCadastroRequisicao(null, null, null, null, null, null, null, null, null);
    }
}
