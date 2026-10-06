package br.org.amigosdonordeste.cadastro.dominio;

/**
 * CPF da responsável. O formato (11 dígitos, com ou sem máscara) já é
 * conferido no DTO; aqui entram os dois dígitos verificadores, que pegam o
 * número digitado errado do papel.
 */
public final class Cpf {

    private Cpf() {}   // classe utilitária: ninguém instancia

    /** A coluna guarda só os 11 dígitos; o front pode mandar "000.000.000-00". */
    public static String somenteDigitos(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.replaceAll("\\D", "");
    }

    /** Recebe só os dígitos (ver somenteDigitos). */
    public static boolean valido(String digitos) {
        if (digitos == null || !digitos.matches("\\d{11}")) {
            return false;
        }
        // 111.111.111-11 e parecidos passam na conta, mas não existem
        if (digitos.chars().distinct().count() == 1) {
            return false;
        }
        return digitoVerificador(digitos, 9) == digitos.charAt(9) - '0'
            && digitoVerificador(digitos, 10) == digitos.charAt(10) - '0';
    }

    /** Dígito da posição `posicao` a partir dos que vêm antes dele (pesos 10..2 e 11..2). */
    private static int digitoVerificador(String digitos, int posicao) {
        int soma = 0;
        for (int i = 0; i < posicao; i++) {
            soma += (digitos.charAt(i) - '0') * (posicao + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}
