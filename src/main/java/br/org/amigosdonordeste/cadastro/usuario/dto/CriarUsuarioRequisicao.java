package br.org.amigosdonordeste.cadastro.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarUsuarioRequisicao(
    @NotBlank @Size(max = 120) String nome,
    @NotBlank @Email(message = "Informe um e-mail válido.") String email,
    @NotBlank @Size(min = 10, message = "A senha precisa ter ao menos 10 caracteres.") String senha
) { }
