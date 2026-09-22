package br.org.amigosdonordeste.cadastro.municipio;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Um município atendido pela associação.
 *
 * O código do IBGE é opcional: o município pode ser cadastrado antes de alguém
 * descobrir o código. Quando ele existe, o mapa usa para desenhar o contorno.
 */
@Entity
@Table(name = "municipio")
public class Municipio {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 2)
    private String uf;

    @Column(name = "codigo_ibge", length = 7, unique = true)
    private String codigoIbge;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }

    public String getCodigoIbge() { return codigoIbge; }
    public void setCodigoIbge(String codigoIbge) { this.codigoIbge = codigoIbge; }

    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(OffsetDateTime criadoEm) { this.criadoEm = criadoEm; }
}
