package br.org.amigosdonordeste.cadastro.comunidade;

import br.org.amigosdonordeste.cadastro.comunidade.enuns.TipoComunidade;
import br.org.amigosdonordeste.cadastro.municipio.Municipio;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "comunidade")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comunidade {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "municipio_id", nullable = false)
    private Municipio municipio;

    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private TipoComunidade tipo = TipoComunidade.SITIO;

    @Column(name = "lider_nome", length = 120)
    private String liderNome;

    @Column(name = "lider_telefone", length = 20)
    private String liderTelefone;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(columnDefinition = "text")
    private String observacoes;
}
