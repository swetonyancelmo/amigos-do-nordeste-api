package br.org.amigosdonordeste.cadastro.usuario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarUsuarioRequisicao(
    @Schema(description = "Nome completo do novo usuário", example = "João Pereira")
    @NotBlank @Size(max = 120) String nome,

    @Schema(description = "E-mail do novo usuário; deve ser único no sistema", example = "joao.pereira@amigosdonordeste.org.br")
    @NotBlank @Email(message = "Informe um e-mail válido.") String email,

    @Schema(description = "Senha inicial do usuário (mínimo 10 caracteres)", example = "SenhaForte789")
    @NotBlank @Size(min = 10, message = "A senha precisa ter ao menos 10 caracteres.") String senha
) { }
