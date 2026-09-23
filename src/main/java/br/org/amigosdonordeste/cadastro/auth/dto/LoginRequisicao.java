package br.org.amigosdonordeste.cadastro.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequisicao(
    @Schema(description = "E-mail cadastrado do usuário", example = "maria.silva@amigosdonordeste.org.br")
    @NotBlank @Email(message = "Informe um e-mail válido.") String email,

    @Schema(description = "Senha do usuário (mínimo 8 caracteres)", example = "SenhaForte123")
    @NotBlank @Size(min = 8, message = "A senha precisa ter ao menos 8 caracteres.") String senha
) { }
