package br.org.amigosdonordeste.cadastro.precadastro;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PreCadastroRepositorio extends JpaRepository<PreCadastro, UUID> {
}
