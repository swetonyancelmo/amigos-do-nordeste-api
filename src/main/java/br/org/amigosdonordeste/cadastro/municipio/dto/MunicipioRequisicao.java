package br.org.amigosdonordeste.cadastro.municipio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Serve para o POST e para o PUT — os campos são os mesmos. */
public record MunicipioRequisicao(

    @NotBlank(message = "Informe o nome do município.")
    @Size(max = 120)
    String nome,

    @NotBlank(message = "Informe a UF.")
    @Pattern(regexp = "[A-Z]{2}", message = "A UF tem 2 letras maiúsculas, como PE.")
    String uf,

    // sem @NotBlank: o código é opcional
    @Pattern(regexp = "\\d{7}", message = "O código do IBGE tem exatamente 7 dígitos.")
    String codigoIbge

) { }
