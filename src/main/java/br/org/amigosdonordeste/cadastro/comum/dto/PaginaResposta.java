package br.org.amigosdonordeste.cadastro.comum.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Página de resultados. Record próprio em vez de devolver o Page do Spring
 * Data: o JSON do PageImpl não é estável entre versões (o próprio Spring
 * avisa no log) e expõe detalhes internos (pageable, sort...).
 */
public record PaginaResposta<T>(
    List<T> itens,
    int pagina,
    int porPagina,
    long total,
    int totalPaginas
) {
    public static <T> PaginaResposta<T> de(Page<?> pagina, List<T> itens) {
        return new PaginaResposta<>(
            itens,
            pagina.getNumber(),
            pagina.getSize(),
            pagina.getTotalElements(),
            pagina.getTotalPages());
    }
}
