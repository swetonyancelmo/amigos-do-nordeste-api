package br.org.amigosdonordeste.cadastro.usuario;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A associacao decidiu na reuniao: uma pessoa cadastra, uma pessoa tem acesso.
 *
 * Por isso esta tabela nao tem perfil, papel nem permissao — nao existe segundo
 * tipo de usuario para diferenciar. E por isso nao existe rota publica de
 * cadastro: a conta nasce do comando de criacao de usuario, rodado uma vez na
 * instalacao. Ver docs/decisoes/ADR-0002.
 *
 * ATENCAO: a resposta do formulario de elicitacao diz "mais de 100" pessoas
 * colaboram com a associacao. Isso ainda precisa ser conciliado com a decisao
 * de acesso unico — ver docs/requisitos.md, questao Q-01.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 160, unique = true)
    private String email;

    /** Hash argon2id. O texto puro da senha nunca entra no banco nem em log. */
    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "ultimo_acesso_em")
    private OffsetDateTime ultimoAcessoEm;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public OffsetDateTime getUltimoAcessoEm() { return ultimoAcessoEm; }
    public void setUltimoAcessoEm(OffsetDateTime ultimoAcessoEm) { this.ultimoAcessoEm = ultimoAcessoEm; }

    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(OffsetDateTime criadoEm) { this.criadoEm = criadoEm; }
}
