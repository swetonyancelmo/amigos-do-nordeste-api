package br.org.amigosdonordeste.cadastro.vulnerabilidade.base;

import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.OperadorFaixa;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.SituacaoSentinela;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.TipoSentinela;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Uma sentinela da Escala de Coelho-Savassi na base de conhecimento (V15).
 * Só o BaseDeConhecimentoService lê isto; o motor recebe a versão validada.
 */
@Entity
@Table(name = "vulnerabilidade_sentinela")
@Getter
@Setter
@NoArgsConstructor
public class RegraSentinela {

    @Id
    @Column(length = 40)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoSentinela tipo;

    /** Só nas BINARIAs. Nas de FAIXA os pontos estão em cada faixa. */
    private Integer pontos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoSentinela situacao;

    @Column(columnDefinition = "text")
    private String justificativa;

    @Column(nullable = false)
    private int ordem;

    // Set: as duas coleções vêm num join fetch só (RegraSentinelaRepositorio)
    @ElementCollection
    @CollectionTable(name = "vulnerabilidade_faixa", joinColumns = @JoinColumn(name = "sentinela_codigo"))
    private Set<FaixaPontuacao> faixas = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "vulnerabilidade_parametro", joinColumns = @JoinColumn(name = "sentinela_codigo"))
    private Set<ParametroSentinela> parametros = new LinkedHashSet<>();

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class FaixaPontuacao {
        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 10)
        private OperadorFaixa operador;

        @Column(nullable = false, precision = 6, scale = 2)
        private BigDecimal limite;

        @Column(nullable = false)
        private int pontos;
    }

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class ParametroSentinela {
        @Column(nullable = false, length = 40)
        private String nome;

        @Column(nullable = false, length = 40)
        private String valor;
    }
}
