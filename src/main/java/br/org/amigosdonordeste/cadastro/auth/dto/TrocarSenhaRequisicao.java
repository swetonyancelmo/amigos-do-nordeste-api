package br.org.amigosdonordeste.cadastro.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrocarSenhaRequisicao(
    @NotBlank String senhaAtual,
    @NotBlank @Size(min = 10, message = "A senha nova precisa ter ao menos 10 caracteres.") String senhaNova
) { }
