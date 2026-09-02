package br.org.amigosdonordeste.cadastro.usuario.dto;

import br.org.amigosdonordeste.cadastro.usuario.Usuario;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Nunca devolve o hash da senha. Entidade não vira resposta de API. */
public record UsuarioResposta(UUID id, String nome, String email, boolean ativo,
                              OffsetDateTime ultimoAcessoEm) {

    public static UsuarioResposta de(Usuario u) {
        return new UsuarioResposta(u.getId(), u.getNome(), u.getEmail(), u.isAtivo(),
            u.getUltimoAcessoEm());
    }
}
