package br.org.amigosdonordeste.cadastro.comum.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ErroResposta(
    @Schema(description = "Data e hora em que o erro ocorreu", example = "2026-09-10T14:30:00-03:00")
    String em,

    @Schema(description = "Código de status HTTP", example = "400")
    int status,

    @Schema(description = "Mensagem descritiva do erro", example = "E-mail já cadastrado")
    String message
) { }
