package br.org.amigosdonordeste.cadastro.usuario;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * Cria a UNICA conta do sistema. Roda uma vez, na instalacao.
 *
 * Este runner existe no lugar de uma rota publica de cadastro: com uma usuaria
 * so, expor um endpoint de registro seria um buraco de seguranca sem beneficio
 * nenhum.
 *
 * Uso:
 *   mvn spring-boot:run -Dspring-boot.run.profiles=criar-usuario
 *   mvn spring-boot:run -Dspring-boot.run.profiles=criar-usuario \
 *       -Dspring-boot.run.arguments=--senha="senha escolhida"
 *
 * A senha aparece UMA VEZ no terminal. Entregue-a a associacao por um canal
 * seguro e peca para trocar no primeiro acesso. O mesmo comando redefine a
 * senha se ela for perdida — e a recuperacao de senha do projeto, ja que nao ha
 * servico de e-mail. Isso precisa estar no manual de entrega.
 */
@Component
@Profile("criar-usuario")
public class CriarUsuarioRunner implements ApplicationRunner {

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder codificador;
    private final String emailInicial;
    private final String nomeInicial;

    public CriarUsuarioRunner(
        UsuarioRepositorio usuarios,
        PasswordEncoder codificador,
        @org.springframework.beans.factory.annotation.Value("${app.usuario-inicial.email:}") String emailInicial,
        @org.springframework.beans.factory.annotation.Value("${app.usuario-inicial.nome:Associação Amigos do Nordeste}") String nomeInicial
    ) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.emailInicial = emailInicial;
        this.nomeInicial = nomeInicial;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (emailInicial == null || emailInicial.isBlank()) {
            throw new IllegalStateException(
                "Defina APP_USUARIO_INICIAL_EMAIL antes de rodar o perfil criar-usuario.");
        }

        String senha = args.containsOption("senha")
            ? args.getOptionValues("senha").get(0)
            : gerarSenha();

        Optional<Usuario> existente = usuarios.findByEmailIgnoreCase(emailInicial);
        Usuario usuario = existente.orElseGet(Usuario::new);
        usuario.setEmail(emailInicial.toLowerCase().trim());
        usuario.setNome(nomeInicial);
        usuario.setSenhaHash(codificador.encode(senha));
        usuario.setAtivo(true);
        usuarios.save(usuario);

        String traco = "-".repeat(56);
        System.out.println();
        System.out.println(existente.isPresent()
            ? "Senha redefinida para " + usuario.getEmail()
            : "Usuária criada: " + usuario.getEmail());
        System.out.println(traco);
        System.out.println("  senha: " + senha);
        System.out.println(traco);
        System.out.println("Anote agora — ela não será mostrada de novo.");
        System.out.println();
    }

    private String gerarSenha() {
        byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
