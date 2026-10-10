package br.org.amigosdonordeste.cadastro.vulnerabilidade.base;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegraEstratoRepositorio extends JpaRepository<RegraEstrato, String> {

    List<RegraEstrato> findAllByOrderByOrdemAsc();
}
