package br.org.amigosdonordeste.cadastro.vulnerabilidade.base;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Corte e rótulo de um estrato na base de conhecimento (V15). */
@Entity
@Table(name = "vulnerabilidade_estrato")
@Getter
@Setter
@NoArgsConstructor
public class RegraEstrato {

    /** name() de EstratoRisco. */
    @Id
    @Column(length = 30)
    private String codigo;

    /** null só em DADOS_INSUFICIENTES. */
    @Column(name = "escore_minimo")
    private Integer escoreMinimo;

    @Column(nullable = false, length = 80)
    private String rotulo;

    @Column(name = "descricao_instrumento", nullable = false, length = 160)
    private String descricaoInstrumento;

    @Column(nullable = false, unique = true)
    private int ordem;
}
