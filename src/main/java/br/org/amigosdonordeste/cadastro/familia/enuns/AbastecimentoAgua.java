package br.org.amigosdonordeste.cadastro.familia.enuns;

/**
 * De onde vem a agua da familia. Multipla escolha: cisterna e carro-pipa
 * convivem, e e o caso comum no sertao — por isso vira tabela auxiliar, nunca
 * coluna. Ver docs/decisoes/ADR-0003.
 *
 * Categorias baseadas na Ficha de Cadastro Domiciliar do e-SUS. Valores
 * provisorios — confirmar e servir por /api/metadados.
 */
public enum AbastecimentoAgua {
    REDE_ENCANADA,
    POCO_OU_NASCENTE,
    CISTERNA,
    CARRO_PIPA,
    AGUA_DA_CHUVA,
    OUTRO
}
