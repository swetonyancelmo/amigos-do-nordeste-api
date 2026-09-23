package br.org.amigosdonordeste.cadastro.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrocarSenhaRequisicao(
    @Schema(description = "Senha atual do usuário, usada para validação", example = "SenhaAntiga123")
    @NotBlank String senhaAtual,

    @Schema(description = "Nova senha desejada (mínimo 10 caracteres)", example = "NovaSenhaForte456")
    @NotBlank @Size(min = 10, message = "A senha nova precisa ter ao menos 10 caracteres.") String senhaNova
) { }
