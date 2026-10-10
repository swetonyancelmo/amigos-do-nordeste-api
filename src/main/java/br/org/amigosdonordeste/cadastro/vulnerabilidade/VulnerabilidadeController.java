package br.org.amigosdonordeste.cadastro.vulnerabilidade;

import br.org.amigosdonordeste.cadastro.vulnerabilidade.base.BaseDeConhecimentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transparência da avaliação de vulnerabilidade (ADR-0010): a tela mostra a
 * tabela de onde a prioridade sugerida sai, com os valores em uso. ADMIN,
 * como toda rota (SegurancaConfig): não abre nada para o token da agente.
 */
@Tag(name = "vulnerabilidade")
@RestController
@RequestMapping("/api/vulnerabilidade")
public class VulnerabilidadeController {

    private final BaseDeConhecimentoService baseDeConhecimento;

    public VulnerabilidadeController(BaseDeConhecimentoService baseDeConhecimento) {
        this.baseDeConhecimento = baseDeConhecimento;
    }

    @Operation(summary = "Regras em uso na avaliação: sentinelas e pontos, faixas, estratos e o que fica de fora (ADR-0010)")
    @GetMapping("/base")
    public BaseDeConhecimentoResposta base() {
        return baseDeConhecimento.descrever();
    }
}
