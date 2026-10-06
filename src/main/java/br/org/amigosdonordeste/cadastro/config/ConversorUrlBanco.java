package br.org.amigosdonordeste.cadastro.config;

import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Aceita em DATABASE_URL a string que o console do Neon mostra, do jeito que
 * ela vem:
 *
 *   postgresql://usuario:senha@ep-xxxx-pooler.regiao.aws.neon.tech/neondb?sslmode=require
 *
 * O JDBC nao entende esse formato e o erro que aparece nao diz isso. Em vez de
 * pedir a quem publica que remonte a URL a mao, a conversao acontece aqui,
 * antes do Spring ler a configuracao: DATABASE_URL vira a URL JDBC e
 * DATABASE_USUARIO / DATABASE_SENHA saem de dentro dela. Uma URL que ja e
 * JDBC (local, CI) passa sem mudanca.
 *
 * Com o host do pooler do Neon (o que tem "-pooler", ADR-0004), o Flyway vai
 * pelo host direto, o mesmo sem "-pooler": migracao nao passa pelo PgBouncer.
 * Se SPRING_FLYWAY_URL estiver definida, ela vale e nada e derivado.
 *
 * Senha nunca vai para o log; o host vai, porque e o que se confere quando a
 * conexao falha.
 */
public class ConversorUrlBanco implements EnvironmentPostProcessor, Ordered {

    static final String VARIAVEL_URL = "DATABASE_URL";
    static final String VARIAVEL_USUARIO = "DATABASE_USUARIO";
    static final String VARIAVEL_SENHA = "DATABASE_SENHA";
    static final String FLYWAY_URL = "spring.flyway.url";

    private static final String JDBC = "jdbc:postgresql://";
    private static final String MARCA_POOLER = "-pooler.";

    private final Log log;

    public ConversorUrlBanco(DeferredLogFactory logs) {
        this.log = logs.getLog(ConversorUrlBanco.class);
    }

    /** O que sai da URL: a JDBC, as credenciais (se vieram nela) e o host direto. */
    record Conexao(String urlJdbc, String usuario, String senha, String urlJdbcDireta) { }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment ambiente, SpringApplication aplicacao) {
        String bruta = ambiente.getProperty(VARIAVEL_URL);
        if (bruta == null || bruta.isBlank()) {
            return;
        }

        Conexao conexao = converter(bruta).orElse(null);
        if (conexao == null) {
            return;
        }

        Map<String, Object> valores = new HashMap<>();
        if (!conexao.urlJdbc().equals(bruta)) {
            valores.put(VARIAVEL_URL, conexao.urlJdbc());
            log.info("DATABASE_URL no formato postgresql:// convertida para JDBC (host "
                + hostDe(conexao.urlJdbc()) + ").");
        }
        if (conexao.usuario() != null) {
            valores.put(VARIAVEL_USUARIO, conexao.usuario());
        }
        if (conexao.senha() != null) {
            valores.put(VARIAVEL_SENHA, conexao.senha());
        }
        if (conexao.urlJdbcDireta() != null && !temValor(ambiente, FLYWAY_URL)) {
            // Usuario e senha do Flyway caem nos do datasource quando ausentes.
            valores.put(FLYWAY_URL, conexao.urlJdbcDireta());
            log.info("Flyway migra pelo host direto do Neon (" + hostDe(conexao.urlJdbcDireta())
                + "), sem o pooler.");
        }

        if (!valores.isEmpty()) {
            ambiente.getPropertySources().addFirst(new MapPropertySource("conversorUrlBanco", valores));
        }
    }

    /**
     * Vazio quando a URL nao e Postgres (H2 dos testes, por exemplo) ou nao
     * da para ler: nesse caso o valor segue como veio e o erro, se houver,
     * aparece na conexao.
     */
    static Optional<Conexao> converter(String bruta) {
        String limpa = limpar(bruta);
        boolean jaEJdbc = limpa.startsWith(JDBC);
        String semJdbc;
        if (jaEJdbc) {
            semJdbc = limpa.substring("jdbc:".length());
        } else if (limpa.startsWith("postgresql://") || limpa.startsWith("postgres://")) {
            semJdbc = limpa;
        } else {
            return Optional.empty();
        }

        URI uri;
        try {
            uri = new URI(semJdbc);
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
        if (uri.getHost() == null) {
            return Optional.empty();
        }

        String usuario = null;
        String senha = null;
        String credenciais = uri.getUserInfo();
        if (!jaEJdbc && credenciais != null) {
            int doisPontos = credenciais.indexOf(':');
            usuario = doisPontos < 0 ? credenciais : credenciais.substring(0, doisPontos);
            senha = doisPontos < 0 ? null : credenciais.substring(doisPontos + 1);
        }

        String resto = (uri.getPort() == -1 ? "" : ":" + uri.getPort())
            + (uri.getRawPath() == null ? "" : uri.getRawPath())
            + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        String urlJdbc = jaEJdbc ? limpa : JDBC + uri.getHost() + resto;

        String direta = uri.getHost().contains(MARCA_POOLER)
            ? JDBC + uri.getHost().replace(MARCA_POOLER, ".") + resto
            : null;

        return Optional.of(new Conexao(urlJdbc, usuario, senha, direta));
    }

    /** Tira espaços, aspas e o "psql" do comando que o console também oferece. */
    private static String limpar(String bruta) {
        String limpa = bruta.trim();
        if (limpa.startsWith("psql ")) {
            limpa = limpa.substring("psql ".length()).trim();
        }
        if (limpa.length() >= 2
            && (limpa.startsWith("'") && limpa.endsWith("'")
                || limpa.startsWith("\"") && limpa.endsWith("\""))) {
            limpa = limpa.substring(1, limpa.length() - 1).trim();
        }
        return limpa;
    }

    private static boolean temValor(ConfigurableEnvironment ambiente, String chave) {
        String valor = ambiente.getProperty(chave);
        return valor != null && !valor.isBlank();
    }

    private static String hostDe(String urlJdbc) {
        String semPrefixo = urlJdbc.substring(JDBC.length());
        int fim = semPrefixo.indexOf('/');
        return fim < 0 ? semPrefixo : semPrefixo.substring(0, fim);
    }

    /** Depois de carregar application.yml e as variaveis de ambiente. */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
