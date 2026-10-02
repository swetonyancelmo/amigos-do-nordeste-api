package br.org.amigosdonordeste.cadastro.pessoa.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Corpo de POST /api/pessoas/{id}/mover. */
@Schema(description = "Para onde a pessoa vai. A comunidade dela muda junto, porque vem da família.")
public record MoverPessoaRequisicao(
        @NotNull @Schema(description = "A nova família, já existente")
        UUID familiaId
) {
}
