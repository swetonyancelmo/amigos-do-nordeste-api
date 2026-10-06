package br.org.amigosdonordeste.cadastro.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/** Hosts e credenciais inventados: nada aqui aponta para banco de verdade. */
class ConversorUrlBancoTest {

    private static final String NEON_POOLER =
        "postgresql://usuario_teste:senha_teste@ep-exemplo-123456-pooler.us-east-1.aws.neon.tech/neondb"
            + "?sslmode=require&channel_binding=require";

    private static final String JDBC_POOLER =
        "jdbc:postgresql://ep-exemplo-123456-pooler.us-east-1.aws.neon.tech/neondb"
            + "?sslmode=require&channel_binding=require";

    private static final String JDBC_DIRETA =
        "jdbc:postgresql://ep-exemplo-123456.us-east-1.aws.neon.tech/neondb"
            + "?sslmode=require&channel_binding=require";

    private final ConversorUrlBanco conversor = new ConversorUrlBanco(Supplier::get);

    @Test
    @DisplayName("string do Neon vira URL JDBC, usuário e senha separados")
    void converteStringDoNeon() {
        var conexao = ConversorUrlBanco.converter(NEON_POOLER).orElseThrow();
        assertEquals(JDBC_POOLER, conexao.urlJdbc());
        assertEquals("usuario_teste", conexao.usuario());
        assertEquals("senha_teste", conexao.senha());
    }

    @Test
    @DisplayName("host com -pooler gera o host direto para o Flyway")
    void derivaHostDireto() {
        var conexao = ConversorUrlBanco.converter(NEON_POOLER).orElseThrow();
        assertEquals(JDBC_DIRETA, conexao.urlJdbcDireta());
    }

    @Test
    @DisplayName("aceita a string colada com aspas ou com o comando psql na frente")
    void toleraAspasEPsql() {
        assertEquals(JDBC_POOLER,
            ConversorUrlBanco.converter("psql '" + NEON_POOLER + "'").orElseThrow().urlJdbc());
        assertEquals(JDBC_POOLER,
            ConversorUrlBanco.converter("  \"" + NEON_POOLER + "\"  ").orElseThrow().urlJdbc());
    }

    @Test
    @DisplayName("prefixo postgres:// também é aceito, e a porta é mantida")
    void aceitaPrefixoCurtoEPorta() {
        var conexao = ConversorUrlBanco.converter("postgres://u:s@db.exemplo.local:6543/base").orElseThrow();
        assertEquals("jdbc:postgresql://db.exemplo.local:6543/base", conexao.urlJdbc());
        assertNull(conexao.urlJdbcDireta());
    }

    @Test
    @DisplayName("senha com caractere especial codificado na URL chega decodificada")
    void decodificaSenha() {
        var conexao = ConversorUrlBanco.converter("postgresql://u:a%40b%3Ac@db.exemplo.local/base").orElseThrow();
        assertEquals("a@b:c", conexao.senha());
    }

    @Test
    @DisplayName("URL que não é Postgres (H2 dos testes) fica de fora")
    void ignoraOutrosBancos() {
        assertTrue(ConversorUrlBanco.converter("jdbc:h2:mem:testdb").isEmpty());
    }

    @Test
    @DisplayName("ambiente: substitui DATABASE_* e aponta o Flyway para o host direto")
    void aplicaNoAmbiente() {
        var ambiente = new MockEnvironment()
            .withProperty("DATABASE_URL", NEON_POOLER)
            .withProperty("DATABASE_USUARIO", "and");

        conversor.postProcessEnvironment(ambiente, null);

        assertEquals(JDBC_POOLER, ambiente.getProperty("DATABASE_URL"));
        assertEquals("usuario_teste", ambiente.getProperty("DATABASE_USUARIO"));
        assertEquals("senha_teste", ambiente.getProperty("DATABASE_SENHA"));
        assertEquals(JDBC_DIRETA, ambiente.getProperty("spring.flyway.url"));
    }

    @Test
    @DisplayName("ambiente: URL JDBC local passa sem mudança nenhuma")
    void naoMexeEmJdbcLocal() {
        var ambiente = new MockEnvironment()
            .withProperty("DATABASE_URL", "jdbc:postgresql://localhost:5432/cadastro")
            .withProperty("DATABASE_USUARIO", "and");

        conversor.postProcessEnvironment(ambiente, null);

        assertFalse(ambiente.getPropertySources().contains("conversorUrlBanco"));
        assertEquals("and", ambiente.getProperty("DATABASE_USUARIO"));
    }

    @Test
    @DisplayName("ambiente: SPRING_FLYWAY_URL definida à mão é respeitada")
    void respeitaFlywayExplicito() {
        var ambiente = new MockEnvironment()
            .withProperty("DATABASE_URL", NEON_POOLER)
            .withProperty("spring.flyway.url", "jdbc:postgresql://outro.exemplo.local/base");

        conversor.postProcessEnvironment(ambiente, null);

        assertEquals("jdbc:postgresql://outro.exemplo.local/base", ambiente.getProperty("spring.flyway.url"));
    }
}
