package br.org.amigosdonordeste.cadastro.agente.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarAgenteRequisicao(
    @Schema(description = "Nome da agente, como o app vai mostrar", example = "Agente de Campo")
    @NotBlank(message = "Informe o nome da agente.") @Size(max = 120) String nome
) { }
