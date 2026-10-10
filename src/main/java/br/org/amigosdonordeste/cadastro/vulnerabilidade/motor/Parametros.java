package br.org.amigosdonordeste.cadastro.vulnerabilidade.motor;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Os parâmetros de adaptação de uma sentinela (vulnerabilidade_parametro):
 * quais valores de enum contam, limites de idade. Guardados como texto no
 * banco; aqui viram tipo, e valor que não converte é base inválida.
 */
public record Parametros(String codigoSentinela, Map<String, List<String>> valores) {

    public Parametros {
        valores = Map.copyOf(valores);
    }

    /** Conjunto de valores de enum. Nome ausente = conjunto vazio (o critério fica desligado). */
    public <E extends Enum<E>> Set<E> enums(String nome, Class<E> tipo) {
        Set<E> resultado = EnumSet.noneOf(tipo);
        for (String valor : valores.getOrDefault(nome, List.of())) {
            try {
                resultado.add(Enum.valueOf(tipo, valor));
            } catch (IllegalArgumentException e) {
                throw new BaseDeConhecimentoInvalidaException(codigoSentinela + "." + nome
                        + ": \"" + valor + "\" não é valor de " + tipo.getSimpleName());
            }
        }
        return resultado;
    }

    /** Um inteiro obrigatório. */
    public int inteiro(String nome) {
        List<String> lista = valores.getOrDefault(nome, List.of());
        if (lista.size() != 1) {
            throw new BaseDeConhecimentoInvalidaException(codigoSentinela + "." + nome
                    + ": esperava um valor, há " + lista.size());
        }
        try {
            return Integer.parseInt(lista.get(0).trim());
        } catch (NumberFormatException e) {
            throw new BaseDeConhecimentoInvalidaException(codigoSentinela + "." + nome
                    + ": \"" + lista.get(0) + "\" não é número inteiro");
        }
    }
}
