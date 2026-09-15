package br.org.amigosdonordeste.cadastro.agente;

import br.org.amigosdonordeste.cadastro.agente.dto.AtivacaoAgenteResposta;
import br.org.amigosdonordeste.cadastro.comum.LimitadorPorIp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Troca o codigo de convite pelo token do aparelho.
 *
 * O codigo e de uso unico, e isso e garantido pelo banco, nao pelo Java: o
 * UPDATE condicional do repositorio so grava o hash se o codigo ainda estiver
 * na linha. Uma segunda tentativa — mesmo simultanea — nao encontra nada e
 * recebe a mesma resposta de um codigo que nunca existiu.
 */
@Service
public class AtivacaoAgenteService {

    private final AgenteRepositorio agentes;
    private final TokenAgenteService tokens;
    private final LimitadorPorIp limitador;

    public AtivacaoAgenteService(AgenteRepositorio agentes,
                                 TokenAgenteService tokens,
                                 @Value("${app.ativacao-agente.max-tentativas}") int maxTentativas,
                                 @Value("${app.ativacao-agente.janela}") Duration janela) {
        this.agentes = agentes;
        this.tokens = tokens;
        this.limitador = new LimitadorPorIp(maxTentativas, janela);
    }

    @Transactional
    public AtivacaoAgenteResposta ativar(String codigo, String ip) {
        // Conta toda tentativa, certa ou errada: o que se quer barrar e a
        // varredura de codigos, e ela nao avisa quando acerta.
        limitador.registrar(ip);

        String token = tokens.gerar();
        String hash = TokenAgenteService.hash(token);

        if (agentes.consumirCodigoConvite(codigo, hash, OffsetDateTime.now()) != 1) {
            throw new CodigoConviteInvalidoException();
        }

        Agente agente = agentes.findByTokenHashAndAtivoTrue(hash)
            .orElseThrow(CodigoConviteInvalidoException::new);

        // O token em claro sai daqui uma vez e nao volta: nem no banco, nem em log.
        return new AtivacaoAgenteResposta(token, agente.getNome());
    }
}
