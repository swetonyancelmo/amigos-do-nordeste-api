package br.org.amigosdonordeste.cadastro.familia;

import br.org.amigosdonordeste.cadastro.comunidade.Comunidade;
import br.org.amigosdonordeste.cadastro.familia.enuns.AbastecimentoAgua;
import br.org.amigosdonordeste.cadastro.familia.enuns.EscoamentoSanitario;
import br.org.amigosdonordeste.cadastro.familia.enuns.TratamentoAgua;
import br.org.amigosdonordeste.cadastro.fonterenda.FonteRenda;
import br.org.amigosdonordeste.cadastro.pessoa.Pessoa;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
    name = "familia",
    indexes = @Index(name = "idx_familia_responsavel_nome", columnList = "responsavel_nome"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Familia {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "comunidade_id", nullable = false)
    private Comunidade comunidade;

    @Column(name = "responsavel_nome", nullable = false, length = 120)
    private String responsavelNome;

    @Column(name = "responsavel_cpf", length = 11)
    private String responsavelCpf;

    @Column(length = 20)
    private String telefone;

    @Column(name = "ponto_referencia", length = 255)
    private String pontoReferencia;

    @Column(name = "tem_banheiro")
    private Boolean temBanheiro;

    @Enumerated(EnumType.STRING)
    @Column(name = "escoamento_sanitario", length = 40)
    private EscoamentoSanitario escoamentoSanitario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tratamento_agua", length = 40)
    private TratamentoAgua tratamentoAgua;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "familia_abastecimento_agua",
        joinColumns = @JoinColumn(name = "familia_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "abastecimento", nullable = false, length = 30)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private Set<AbastecimentoAgua> abastecimentoAgua = new LinkedHashSet<>();

    @OneToMany(mappedBy = "familia", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private List<Pessoa> pessoas = new ArrayList<>();

    @OneToMany(mappedBy = "familia", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private List<FonteRenda> fontesRenda = new ArrayList<>();

    @Column(columnDefinition = "text")
    private String observacoes;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @PrePersist
    private void aoPersistir() {
        OffsetDateTime agora = OffsetDateTime.now();
        if (criadoEm == null) {
            criadoEm = agora;
        }
        atualizadoEm = agora;
    }

    @PreUpdate
    private void aoAtualizar() {
        atualizadoEm = OffsetDateTime.now();
    }

    public void adicionarPessoa(Pessoa pessoa) {
        pessoa.setFamilia(this);
        pessoas.add(pessoa);
    }

    public void removerPessoa(Pessoa pessoa) {
        pessoas.remove(pessoa);
        pessoa.setFamilia(null);
    }

    public void adicionarFonteRenda(FonteRenda fonte) {
        fonte.setFamilia(this);
        fontesRenda.add(fonte);
    }

    public void removerFonteRenda(FonteRenda fonte) {
        fontesRenda.remove(fonte);
        fonte.setFamilia(null);
    }
}
