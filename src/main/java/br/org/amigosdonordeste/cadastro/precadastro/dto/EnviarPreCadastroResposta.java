package br.org.amigosdonordeste.cadastro.precadastro.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record EnviarPreCadastroResposta(
    @Schema(description = "O mesmo id que o aparelho mandou")
    UUID id,

    @Schema(description = "ACEITO na primeira vez; JA_RECEBIDO nas seguintes. O app trata os dois como sucesso.")
    ResultadoEnvio situacao
) { }
