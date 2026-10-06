package br.org.amigosdonordeste.cadastro.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Números fictícios: só a conta dos dígitos verificadores importa. */
class CpfTest {

    @Test
    @DisplayName("aceita CPF com os dois dígitos verificadores certos")
    void aceitaCpfValido() {
        assertTrue(Cpf.valido("12345678909"));
        assertTrue(Cpf.valido("11144477735"));
    }

    @Test
    @DisplayName("recusa CPF com dígito verificador errado")
    void recusaDigitoErrado() {
        assertFalse(Cpf.valido("12345678900"));
        assertFalse(Cpf.valido("12345678919"));
    }

    @Test
    @DisplayName("recusa sequência de um dígito só, mesmo passando na conta")
    void recusaRepetido() {
        assertFalse(Cpf.valido("11111111111"));
        assertFalse(Cpf.valido("00000000000"));
    }

    @Test
    @DisplayName("recusa o que não tem 11 dígitos")
    void recusaTamanhoErrado() {
        assertFalse(Cpf.valido(null));
        assertFalse(Cpf.valido("1234567890"));
        assertFalse(Cpf.valido("123.456.789-09"));
    }

    @Test
    @DisplayName("somenteDigitos tira a máscara e trata vazio como ausente")
    void somenteDigitos() {
        assertEquals("12345678909", Cpf.somenteDigitos("123.456.789-09"));
        assertNull(Cpf.somenteDigitos("  "));
        assertNull(Cpf.somenteDigitos(null));
    }
}
