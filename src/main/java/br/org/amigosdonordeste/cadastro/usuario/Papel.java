package br.org.amigosdonordeste.cadastro.usuario;

/**
 * Papel do usuario do painel. Vira authority ROLE_&lt;papel&gt; no Spring Security.
 *
 * AGENTE nao esta aqui de proposito: a agente de campo nao tem conta nem senha,
 * ela tem um aparelho com token (ver pacote agente). O papel dela e dado pelo
 * FiltroTokenAgente, nao por esta tabela.
 */
public enum Papel {
    ADMIN
}
