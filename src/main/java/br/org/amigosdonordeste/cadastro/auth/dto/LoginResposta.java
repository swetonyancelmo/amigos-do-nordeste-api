package br.org.amigosdonordeste.cadastro.auth.dto;

import java.util.UUID;

public record LoginResposta(UsuarioResumo usuario, String accessToken) {

    public record UsuarioResumo(UUID id, String nome, String email) { }
}
