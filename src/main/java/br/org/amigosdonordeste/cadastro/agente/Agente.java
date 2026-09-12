package br.org.amigosdonordeste.cadastro.agente;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Agente de campo: a pessoa que anda pela comunidade com o celular e envia
 * pre-cadastros. Nao e usuario do painel — nao tem e-mail nem senha.
 *
 * O acesso nasce de um codigo de convite de seis digitos (uso unico) trocado
 * por um token de aparelho. O banco guarda so o hash do token (V8).
 */
@Entity
@Table(name = "agente")
public class Agente {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    /** Vira null assim que e trocado pelo token — uso unico. */
    @Column(name = "codigo_convite", length = 6, unique = true)
    private String codigoConvite;

    /** SHA-256 do token do aparelho. O token em claro nunca entra no banco. */
    @Column(name = "token_hash", length = 255)
    private String tokenHash;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "ativado_em")
    private OffsetDateTime ativadoEm;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCodigoConvite() { return codigoConvite; }
    public void setCodigoConvite(String codigoConvite) { this.codigoConvite = codigoConvite; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public OffsetDateTime getAtivadoEm() { return ativadoEm; }
    public void setAtivadoEm(OffsetDateTime ativadoEm) { this.ativadoEm = ativadoEm; }

    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(OffsetDateTime criadoEm) { this.criadoEm = criadoEm; }
}
