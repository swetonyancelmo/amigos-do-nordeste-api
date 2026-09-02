package br.org.amigosdonordeste.cadastro.auth;

import br.org.amigosdonordeste.cadastro.usuario.Usuario;
import br.org.amigosdonordeste.cadastro.usuario.UsuarioRepositorio;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder codificador;
    private final JwtService jwt;

    public AuthService(UsuarioRepositorio usuarios, PasswordEncoder codificador, JwtService jwt) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.jwt = jwt;
    }

    public record Tokens(String acesso, String renovacao, Usuario usuario) { }

    @Transactional
    public Tokens entrar(String email, String senha) {
        Optional<Usuario> encontrado = usuarios.findByEmailIgnoreCaseAndAtivoTrue(email.trim());

        // Mensagem identica para e-mail inexistente e senha errada: dizer qual
        // dos dois falhou entrega ao atacante metade da resposta de graca.
        if (encontrado.isEmpty()) {
            // Gasta o mesmo tempo de uma verificacao real, para o tempo de
            // resposta nao revelar se o e-mail existe.
            codificador.encode("descarte-tempo-constante");
            throw new CredenciaisInvalidasException();
        }

        Usuario usuario = encontrado.get();
        if (!codificador.matches(senha, usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException();
        }

        usuario.setUltimoAcessoEm(OffsetDateTime.now());
        usuarios.save(usuario);

        return new Tokens(
            jwt.gerarAcesso(usuario.getId(), usuario.getEmail()),
            jwt.gerarRenovacao(usuario.getId(), usuario.getEmail()),
            usuario);
    }

    @Transactional(readOnly = true)
    public Tokens renovar(String tokenRenovacao) {
        if (tokenRenovacao == null || tokenRenovacao.isBlank()) {
            throw new CredenciaisInvalidasException("Sessão expirada. Entre novamente.");
        }

        Claims claims;
        try {
            claims = jwt.ler(tokenRenovacao);
        } catch (JwtException e) {
            throw new CredenciaisInvalidasException("Sessão expirada. Entre novamente.");
        }

        if (!JwtService.TIPO_RENOVACAO.equals(claims.get("tipo", String.class))) {
            throw new CredenciaisInvalidasException();
        }

        Usuario usuario = usuarios.findById(UUID.fromString(claims.getSubject()))
            .filter(Usuario::isAtivo)
            .orElseThrow(CredenciaisInvalidasException::new);

        return new Tokens(
            jwt.gerarAcesso(usuario.getId(), usuario.getEmail()),
            jwt.gerarRenovacao(usuario.getId(), usuario.getEmail()),
            usuario);
    }

    @Transactional
    public void trocarSenha(UUID usuarioId, String senhaAtual, String senhaNova) {
        Usuario usuario = usuarios.findById(usuarioId).orElseThrow(CredenciaisInvalidasException::new);
        if (!codificador.matches(senhaAtual, usuario.getSenhaHash())) {
            throw new SenhaAtualIncorretaException();
        }
        usuario.setSenhaHash(codificador.encode(senhaNova));
        usuarios.save(usuario);
    }
}
