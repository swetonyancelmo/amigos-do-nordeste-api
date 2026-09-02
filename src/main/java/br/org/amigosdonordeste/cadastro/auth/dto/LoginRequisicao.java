package br.org.amigosdonordeste.cadastro.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequisicao(
    @NotBlank @Email(message = "Informe um e-mail válido.") String email,
    @NotBlank @Size(min = 8, message = "A senha precisa ter ao menos 8 caracteres.") String senha
) { }
