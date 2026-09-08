package br.org.amigosdonordeste.cadastro.dominio;

/**
 * Rotulavel:
 * Interface comum aos enums que possuem um rótulo para exibição.
 * Permite que os diferentes enums sejam tratados de forma padronizada
 * pelo endpoint de metadados.
 *
 * /** Todo enum de domínio que aparece em GET /api/metadados. */
public interface Rotulavel {

    String name();

    String getRotulo();

}