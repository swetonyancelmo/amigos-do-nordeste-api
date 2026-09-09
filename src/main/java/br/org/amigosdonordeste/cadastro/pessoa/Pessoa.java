package br.org.amigosdonordeste.cadastro.pessoa;

import br.org.amigosdonordeste.cadastro.familia.Familia;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Parentesco;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Sexo;
import br.org.amigosdonordeste.cadastro.pessoa.enums.TamanhoRoupa;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pessoa")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pessoa {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "familia_id", nullable = false)
    private Familia familia;

    public void setFamilia(Familia familia) {
        this.familia = familia;
    }

    @Column(length = 120)
    private String nome;

    @Column(name = "cadastro_incompleto", nullable = false)
    @Builder.Default
    private boolean cadastroIncompleto = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Sexo sexo;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "idade_estimada")
    private Integer idadeEstimada;

    @Column(name = "idade_estimada_em")
    private LocalDate idadeEstimadaEm;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Parentesco parentesco;

    private Boolean estuda;

    @Column(length = 40)
    private String serie;

    @Enumerated(EnumType.STRING)
    @Column(name = "tamanho_roupa", length = 20)
    private TamanhoRoupa tamanhoRoupa;

    @Column(name = "numero_calcado")
    private String numeroCalcado;

    private Boolean gestante;

    @Column(columnDefinition = "text")
    private String observacoes;
}
