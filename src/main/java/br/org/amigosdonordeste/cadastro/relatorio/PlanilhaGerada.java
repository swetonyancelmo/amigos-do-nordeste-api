package br.org.amigosdonordeste.cadastro.relatorio;

/**
 * Issue #21: o .xlsx pronto (bytes) junto do nome de arquivo sugerido —
 * carrega os dois juntos do service até o controller montar o
 * Content-Disposition.
 */
public record PlanilhaGerada(byte[] conteudo, String nomeArquivo) {
}
