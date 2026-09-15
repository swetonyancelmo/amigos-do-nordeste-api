package br.org.amigosdonordeste.cadastro.agente.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AtivarAgenteRequisicao(
    @Schema(description = "Código de convite de seis dígitos entregue à agente", example = "472916")
    @NotBlank @Pattern(regexp = "\\d{6}", message = "O código tem seis dígitos.") String codigo
) { }
