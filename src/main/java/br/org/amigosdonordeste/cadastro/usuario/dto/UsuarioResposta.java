package br.org.amigosdonordeste.cadastro.usuario.dto;

import br.org.amigosdonordeste.cadastro.usuario.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Nunca devolve o hash da senha. Entidade não vira resposta de API. */
public record UsuarioResposta(
    @Schema(description = "Identificador único do usuário", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    UUID id,

    @Schema(description = "Nome completo do usuário", example = "Maria da Silva")
    String nome,

    @Schema(description = "E-mail do usuário", example = "maria.silva@amigosdonordeste.org.br")
    String email,

    @Schema(description = "Indica se o usuário está ativo no sistema", example = "true")
    boolean ativo,

    @Schema(description = "Data e hora do último acesso do usuário; nulo se nunca acessou", example = "2026-09-10T14:30:00-03:00")
    OffsetDateTime ultimoAcessoEm) {

    public static UsuarioResposta de(Usuario u) {
        return new UsuarioResposta(u.getId(), u.getNome(), u.getEmail(), u.isAtivo(),
            u.getUltimoAcessoEm());
    }
}
