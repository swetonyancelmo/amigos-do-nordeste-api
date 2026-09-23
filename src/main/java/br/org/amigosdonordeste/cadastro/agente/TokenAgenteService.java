package br.org.amigosdonordeste.cadastro.agente;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Gera e confere o token do aparelho da agente.
 *
 * O hash e SHA-256 sem sal, e isso e proposital: o filtro precisa ACHAR o
 * agente pelo hash a cada requisicao, e um hash com sal (argon2, bcrypt) nao
 * permite busca. Sal existe para proteger senha fraca escolhida por gente;
 * este token tem 256 bits aleatorios, entao forca bruta sobre o hash nao e
 * um vetor real. O que importa e que o token em claro nunca entre no banco
 * nem em log — so o hash.
 *
 * O prefixo `agente_` deixa o formato reconhecivel: cada filtro cuida do seu
 * (JWT tem tres partes com ponto; este nao tem ponto nenhum).
 */
@Service
public class TokenAgenteService {

    public static final String PREFIXO = "agente_";

    private final SecureRandom aleatorio = new SecureRandom();

    public static boolean eTokenDeAgente(String token) {
        return token != null && token.startsWith(PREFIXO);
    }

    /** Token novo, em claro. Entregue ao aparelho uma vez e guarde so o hash. */
    public String gerar() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return PREFIXO + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel na JVM", e);
        }
    }
}
