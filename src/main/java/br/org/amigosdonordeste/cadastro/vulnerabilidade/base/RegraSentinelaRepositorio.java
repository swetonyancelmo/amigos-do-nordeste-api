package br.org.amigosdonordeste.cadastro.vulnerabilidade.base;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RegraSentinelaRepositorio extends JpaRepository<RegraSentinela, String> {

    /**
     * A base inteira numa consulta: faixas e parâmetros no mesmo join fetch.
     * São Sets, então não há MultipleBagFetchException, e o produto
     * cartesiano é de poucas dezenas de linhas. Sem isso, cada sentinela
     * faria duas consultas a mais em toda listagem de famílias.
     */
    @Query("""
        select distinct s from RegraSentinela s
        left join fetch s.faixas
        left join fetch s.parametros
        order by s.ordem
        """)
    List<RegraSentinela> buscarBaseCompleta();
}
