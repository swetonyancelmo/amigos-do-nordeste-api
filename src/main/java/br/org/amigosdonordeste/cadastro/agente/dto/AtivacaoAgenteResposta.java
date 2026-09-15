package br.org.amigosdonordeste.cadastro.agente.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AtivacaoAgenteResposta(
    @Schema(description = "Token do aparelho. Entregue uma única vez: o banco guarda só o hash.",
        example = "agente_x7Kp...")
    String token,

    @Schema(description = "Nome da agente, para o aplicativo mostrar quem está logada", example = "Agente de Campo")
    String nomeAgente
) { }
