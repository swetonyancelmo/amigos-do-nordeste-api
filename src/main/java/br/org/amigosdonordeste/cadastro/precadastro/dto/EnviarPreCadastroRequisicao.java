package br.org.amigosdonordeste.cadastro.precadastro.dto;

import br.org.amigosdonordeste.cadastro.pessoa.enums.Sexo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Corpo do POST /api/pre-cadastros. Diferente do POST /api/familias, aqui os
 * ids VEM no payload: o aparelho os gera offline. O da familia e a chave de
 * idempotencia; os das pessoas so acompanham para o app reconciliar depois.
 *
 * O payload inteiro e guardado como chegou (pre_cadastro.payload), entao o
 * que nao e validado aqui tambem nao se perde.
 */
public record EnviarPreCadastroRequisicao(
    @Schema(description = "UUID gerado no aparelho. Reenviar o mesmo id nunca cria outro registro.",
        example = "9f3c2a1e-5b7d-4c8e-9a0f-1b2c3d4e5f60")
    @NotNull UUID id,

    @Schema(example = "Responsável de Exemplo")
    @NotBlank @Size(max = 120) String responsavelNome,

    @Schema(example = "87999990000")
    @Size(max = 20) String telefone,

    @Schema(description = "Id da comunidade, se o aparelho a conhecia. Se o servidor não a encontrar, "
        + "o pré-cadastro entra sem comunidade e o nome fica no payload para a revisão.")
    UUID comunidadeId,

    @Schema(description = "Nome da comunidade como a agente a chamou", example = "Sítio Exemplo")
    @Size(max = 120) String comunidadeNome,

    @Size(max = 255) String pontoReferencia,

    @Schema(description = "Quando o cadastro foi feito no aparelho (pode ser dias antes do envio)",
        example = "2026-09-12T09:12:00Z")
    @NotNull OffsetDateTime criadoEm,

    @NotNull @Valid List<PessoaPreCadastro> pessoas
) {

    public record PessoaPreCadastro(
        @Schema(description = "UUID gerado no aparelho")
        @NotNull UUID id,

        @Schema(description = "Pode faltar quando cadastroIncompleto é true (RF-09)")
        @Size(max = 120) String nome,

        @Schema(description = "Marca uma pessoa da qual a agente não conseguiu tudo — ex. \"filha da responsável\", sem nome")
        Boolean cadastroIncompleto,

        Sexo sexo,

        LocalDate dataNascimento,

        @PositiveOrZero Integer idadeEstimada,

        @Schema(description = "Obrigatória quando idadeEstimada vier: sem ela a idade não envelhece")
        LocalDate idadeEstimadaEm
    ) { }
}
