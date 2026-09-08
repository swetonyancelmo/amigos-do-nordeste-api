package br.org.amigosdonordeste.cadastro.fonterenda;

import br.org.amigosdonordeste.cadastro.familia.Familia;
import br.org.amigosdonordeste.cadastro.fonterenda.enuns.FaixaRenda;
import br.org.amigosdonordeste.cadastro.fonterenda.enuns.TipoFonteRenda;
import br.org.amigosdonordeste.cadastro.pessoa.Pessoa;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "fonte_renda")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FonteRenda {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "familia_id", nullable = false)
    private Familia familia;

    // Setter escrito na mao: o do Lombok nao fica visivel para Familia.java
    // nesta combinacao de Maven + JDK (o unico lugar do projeto que chama um
    // setter Lombok de outra classe).
    public void setFamilia(Familia familia) {
        this.familia = familia;
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoFonteRenda tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pessoa_id")
    private Pessoa pessoa;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private FaixaRenda faixa;

    @Column(columnDefinition = "text")
    private String observacao;
}
