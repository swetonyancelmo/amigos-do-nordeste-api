package br.org.amigosdonordeste.cadastro.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record LoginResposta(
    @Schema(description = "Dados resumidos do usuário autenticado")
    UsuarioResumo usuario,

    @Schema(description = "Token JWT de acesso; deve ser enviado no header Authorization como 'Bearer <token>'",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0In0.abc123")
    String accessToken
) {

    public record UsuarioResumo(
        @Schema(description = "Identificador único do usuário", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Nome completo do usuário", example = "Maria da Silva")
        String nome,

        @Schema(description = "E-mail do usuário", example = "maria.silva@amigosdonordeste.org.br")
        String email
    ) { }
}
