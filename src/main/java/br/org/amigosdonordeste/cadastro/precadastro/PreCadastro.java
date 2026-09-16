package br.org.amigosdonordeste.cadastro.precadastro;

import br.org.amigosdonordeste.cadastro.agente.Agente;
import br.org.amigosdonordeste.cadastro.comunidade.Comunidade;
import br.org.amigosdonordeste.cadastro.familia.Familia;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Familia enviada pelo aparelho da agente, esperando a associacao revisar.
 *
 * O id NAO e gerado aqui: vem do aparelho, que o cria offline. E ele que faz o
 * reenvio ser inofensivo — a agente toca duas vezes em "enviar", ou a internet
 * cai antes de a resposta chegar e o app tenta de novo — e o servidor responde
 * JA_RECEBIDO em vez de gravar a mesma familia outra vez.
 *
 * O payload fica guardado inteiro (jsonb) como a agente mandou. So vira
 * familia/pessoa de verdade quando alguem do painel aprovar.
 */
@Entity
@Table(name = "pre_cadastro")
public class PreCadastro implements Persistable<UUID> {

    @Id
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "agente_id", nullable = false)
    private Agente agente;

    /** Nula quando o aparelho mandou uma comunidade que o servidor nao conhece. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comunidade_id")
    private Comunidade comunidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoPreCadastro situacao = SituacaoPreCadastro.PENDENTE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String payload;

    /** Preenchida na aprovacao, quando o pre-cadastro vira familia. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "familia_id")
    private Familia familia;

    @Column(name = "motivo_devolucao", columnDefinition = "text")
    private String motivoDevolucao;

    @Column(name = "recebido_em", nullable = false)
    private OffsetDateTime recebidoEm = OffsetDateTime.now();

    @Column(name = "avaliado_em")
    private OffsetDateTime avaliadoEm;

    /**
     * Como o id ja vem preenchido, o Spring Data acharia que a entidade existe
     * e faria merge (SELECT + INSERT). Persistable diz que e nova: vai direto
     * de INSERT, e um id repetido bate na chave primaria em vez de virar UPDATE.
     */
    @Transient
    private boolean novo = true;

    @Override
    public boolean isNew() { return novo; }

    @PostLoad
    @PostPersist
    void marcarComoPersistido() { novo = false; }

    @Override
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Agente getAgente() { return agente; }
    public void setAgente(Agente agente) { this.agente = agente; }

    public Comunidade getComunidade() { return comunidade; }
    public void setComunidade(Comunidade comunidade) { this.comunidade = comunidade; }

    public SituacaoPreCadastro getSituacao() { return situacao; }
    public void setSituacao(SituacaoPreCadastro situacao) { this.situacao = situacao; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public Familia getFamilia() { return familia; }
    public void setFamilia(Familia familia) { this.familia = familia; }

    public String getMotivoDevolucao() { return motivoDevolucao; }
    public void setMotivoDevolucao(String motivoDevolucao) { this.motivoDevolucao = motivoDevolucao; }

    public OffsetDateTime getRecebidoEm() { return recebidoEm; }
    public void setRecebidoEm(OffsetDateTime recebidoEm) { this.recebidoEm = recebidoEm; }

    public OffsetDateTime getAvaliadoEm() { return avaliadoEm; }
    public void setAvaliadoEm(OffsetDateTime avaliadoEm) { this.avaliadoEm = avaliadoEm; }
}
