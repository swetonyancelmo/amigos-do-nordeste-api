package br.org.amigosdonordeste.cadastro.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IdadeTest {

    /** Data fixa de propósito: o teste tem que dar o mesmo resultado sempre. */
    private static final LocalDate HOJE = LocalDate.of(2026, 9, 9);

    @Test
    @DisplayName("calcula a idade a partir da data de nascimento")
    void idadePelaDataDeNascimento() {
        Integer idade = Idade.calcular(LocalDate.of(2010, 3, 15), null, null, HOJE);
        assertEquals(16, idade);
    }

    @Test
    @DisplayName("não conta aniversário que ainda não chegou no ano corrente")
    void naoContaAniversarioQueNaoChegou() {
        // nasceu em 31/12/2010; em 09/09/2026 ainda tem 15, não 16
        Integer idade = Idade.calcular(LocalDate.of(2010, 12, 31), null, null, HOJE);
        assertEquals(15, idade);
    }

    @Test
    @DisplayName("estimativa de 4 anos feita há 2 anos vira 6")
    void estimativaEnvelheceSozinha() {
        Integer idade = Idade.calcular(null, 4, LocalDate.of(2024, 9, 9), HOJE);
        assertEquals(6, idade);
    }

    @Test
    @DisplayName("estimativa sem data de referência não serve para nada")
    void estimativaSemDataDeReferencia() {
        assertNull(Idade.calcular(null, 4, null, HOJE));
    }

    @Test
    @DisplayName("sem data de nascimento e sem estimativa, a resposta é não sei")
    void semNadaRetornaNulo() {
        assertNull(Idade.calcular(null, null, null, HOJE));
    }

    // --- os três abaixo não estão nos critérios de aceite, mas valem ---

    @Test
    @DisplayName("aniversário exatamente hoje já conta")
    void aniversarioHoje() {
        assertEquals(16, Idade.calcular(LocalDate.of(2010, 9, 9), null, null, HOJE));
    }

    @Test
    @DisplayName("estimativa só envelhece em ano completo")
    void estimativaSoEnvelheceEmAnoCompleto() {
        // estimada em 01/12/2024: em 09/09/2026 ainda não fez 2 anos completos
        assertEquals(5, Idade.calcular(null, 4, LocalDate.of(2024, 12, 1), HOJE));
    }

    @Test
    @DisplayName("data de nascimento ganha da estimativa quando as duas existem")
    void dataGanhaDaEstimativa() {
        Integer idade = Idade.calcular(LocalDate.of(2000, 1, 1), 4, LocalDate.of(2024, 9, 9), HOJE);
        assertEquals(26, idade);
    }
}
