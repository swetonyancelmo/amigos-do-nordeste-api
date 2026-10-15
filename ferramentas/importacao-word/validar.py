#!/usr/bin/env python3
"""Etapa 2 — confere a planilha extraída e prepara a revisão humana.

Uso:
    python validar.py                                   # extraido.xlsx -> revisao.xlsx
    python validar.py --entrada trabalho/revisao.xlsx   # depois de revisar, de novo

Gera:
  - trabalho/revisao.xlsx: a planilha com as colunas "gravidade" e "problemas",
    e cada célula com problema pintada (vermelho = bloqueia, amarelo = aviso)
    e comentada. Colunas de cabeçalho azul são as que a revisão pode editar.
  - trabalho/relatorio-validacao.md: números do problema, sem nome e sem CPF.

Rodar de novo sobre a revisao.xlsx mantém o que a pessoa editou nas colunas
azuis e recalcula só o diagnóstico.
"""
from __future__ import annotations

import argparse
import sys
from collections import Counter, defaultdict
from datetime import date
from pathlib import Path

import comum
from comum import AVISO, ERRO, eh_xx, ler_crianca, numero_inteiro, texto


def preparar(entrada: Path) -> tuple[list[dict], list[dict], dict[int, comum.Diagnostico]]:
    docs = comum.ler_aba(entrada, comum.ABA_DOCUMENTOS)
    linhas = comum.ler_aba(entrada, comum.ABA_FORMATO_A)
    for l in linhas:
        for c in comum.COLUNAS_A:
            l.setdefault(c, None)
        comum.propor_revisao(l)
    diags = comum.diagnosticar(linhas)
    for i, l in enumerate(linhas):
        dg = diags[i]
        l["cpf_situacao"] = dg.cpf_situacao
        l["gravidade"] = dg.gravidade
        l["problemas"] = " | ".join(f"{p.gravidade} [{p.coluna}] {p.texto}" for p in dg.problemas)
    return docs, linhas, diags


# ---------------------------------------------------------------- relatório

def pct(n: int, total: int) -> str:
    return f"{n} ({n * 100 / total:.0f}%)" if total else str(n)


def relatorio(docs: list[dict], linhas: list[dict], diags: dict[int, comum.Diagnostico]) -> str:
    total = len(linhas)
    por_doc: dict[str, list[dict]] = defaultdict(list)
    for l in linhas:
        por_doc[(l["arquivo"], l["sitio"])].append(l)

    pessoas_decl = sum(numero_inteiro(texto(l["total_pessoas"])) or 0 for l in linhas)
    total_desconhecido = [l for l in linhas if numero_inteiro(texto(l["total_pessoas"])) is None]

    # XX por coluna
    xx = Counter()
    linhas_com_xx = 0
    for l in linhas:
        cols = [c for c in comum.COLUNAS_CRU if eh_xx(texto(l[c]))]
        xx.update(cols)
        linhas_com_xx += bool(cols)

    # crianças
    formas = Counter()
    exemplos_outro: list[str] = []
    so_idade = 0
    nao_fecha = 0
    for i, l in enumerate(linhas):
        cel = [ler_crianca(texto(l[c])) for c in ("crianca_ate_12_a", "crianca_ate_12_b")]
        for c, col in zip(cel, ("crianca_ate_12_a", "crianca_ate_12_b")):
            formas[c.tipo] += 1
            v = texto(l[col])
            if c.tipo in ("OUTRO", "NUMERO") and v not in exemplos_outro:
                exemplos_outro.append(v)
        if any("provavelmente é idade" in p.texto for p in diags[i].problemas):
            nao_fecha += 1
        if any(c.tipo == "OUTRO" for c in cel) or any(c.tipo == "NUMERO_SEXO" and c.numero > 12 for c in cel):
            so_idade += 1
    propostas = sum(1 for l in linhas if texto(l.get("meninos_ate_12")) or texto(l.get("meninas_ate_12")))

    cpf = Counter(l["cpf_situacao"] for l in linhas)
    cpf_repetido = sum(1 for d in diags.values() if any("mesmo CPF" in p.texto for p in d.problemas))
    nome_repetido = [i for i, d in diags.items() if any("mesmo nome" in p.texto for p in d.problemas)]
    entre_docs = sum(1 for i in nome_repetido
                     if any(l2["arquivo"] != linhas[i]["arquivo"] for l2 in linhas
                            if comum.normalizar(texto(l2["responsavel"])) == comum.normalizar(texto(linhas[i]["responsavel"]))
                            and l2 is not linhas[i]))

    adultos_vazios = sum(1 for l in linhas if not texto(l["maior_60"]) and not texto(l["de_13_a_60"]))
    gestante = sum(1 for l in linhas if texto(l["gestante"]))
    erros = sum(1 for d in diags.values() if d.gravidade == ERRO)
    avisos = sum(1 for d in diags.values() if d.gravidade == AVISO)
    limpas = total - erros - avisos

    cab_vazio = sum(1 for d in docs if not texto(d.get("total_familias_cabecalho"))
                    and not texto(d.get("total_criancas_cabecalho")))
    contato_vazio = sum(1 for d in docs if not texto(d.get("contato")))
    em_branco = sum(int(d.get("linhas_numeradas_em_branco") or 0) for d in docs)

    o = []
    w = o.append
    w("# Diagnóstico dos cadastros em Word da associação\n")
    w(f"Gerado em {date.today().isoformat()} pela ferramenta `ferramentas/importacao-word` "
      "(etapa 2, validação). Este relatório traz só números e exemplos sem identificação: "
      "nenhum nome e nenhum CPF.\n")
    w("Os cadastros que a associação mantém hoje são tabelas de Word, uma por sítio ou povoado, "
      "feitas para a campanha de Natal. Antes de importá-los para o sistema, cada linha foi lida "
      "e conferida. Os números abaixo medem o problema que o sistema resolve: informação "
      "contada à mão, totais que ninguém preenche e campos que cada pessoa anota do seu jeito.\n")

    w("## Resumo\n")
    w("| | |\n|---|---|")
    w(f"| Documentos lidos | {len(docs)} |")
    w(f"| Famílias (linhas com dados) | {total} |")
    w(f"| Pessoas declaradas na coluna \"Total de Pessoas\" | {pessoas_decl} |")
    w(f"| Famílias sem total de pessoas (XX ou em branco) | {pct(len(total_desconhecido), total)} |")
    w(f"| Linhas com pelo menos um XX | {pct(linhas_com_xx, total)} |")
    w(f"| Famílias prontas para importar sem nenhuma ressalva | {pct(limpas, total)} |")
    w(f"| Famílias que importam com aviso | {pct(avisos, total)} |")
    w(f"| Famílias bloqueadas até a revisão | {pct(erros, total)} |")
    w("")

    w("## Por documento\n")
    w("| Sítio / povoado | Famílias | Pessoas declaradas | Total XX ou vazio | Linhas numeradas em branco | Município | Data de referência |")
    w("|---|---:|---:|---:|---:|---|---|")
    for d in docs:
        ls = por_doc.get((d["arquivo"], d["sitio"]), [])
        p = sum(numero_inteiro(texto(l["total_pessoas"])) or 0 for l in ls)
        tx = sum(1 for l in ls if numero_inteiro(texto(l["total_pessoas"])) is None)
        w(f"| {d['sitio']} | {len(ls)} | {p} | {tx} | {d.get('linhas_numeradas_em_branco') or 0} | "
          f"{d['municipio']} | {d['data_referencia']} |")
    w("")

    w("## O cabeçalho de totais vem em branco\n")
    w("Cada documento abre com um quadro de \"Total de famílias\" e \"Total de crianças\" "
      "(o rótulo original diz \"TOTA DE CRIANÇAS\"). "
      f"Em **{cab_vazio} de {len(docs)}** documentos os dois totais estão em branco, e em "
      f"**{contato_vazio} de {len(docs)}** o contato também. Ninguém fecha a conta à mão. "
      "Não é erro de leitura: é o dado como ele está. No sistema esses totais são consulta, "
      "nunca digitados.\n")
    w(f"Os documentos também trazem **{em_branco} linhas numeradas e vazias** (o modelo já vem "
      "com a numeração até o fim da página). Elas foram ignoradas.\n")

    w("## O que significa \"XX\"\n")
    w("\"XX\" aparece nos documentos e ninguém documentou o que quer dizer: pode ser \"não "
      "informado\", \"não tem\" ou uma marca de conferência. A importação **não interpreta**: "
      "trata como valor desconhecido e pergunta.\n")
    w("| Coluna | Células com XX |\n|---|---:|")
    for c, n in xx.most_common():
        w(f"| {c} | {n} |")
    w("")
    w("Na coluna de CPF aparece como \"XXX\"; na de total de pessoas, como \"XX\". Na coluna de "
      "crianças, \"XX\" em uma sub-coluna convive com número na outra, o que sugere \"nenhuma "
      "criança deste lado\", mas isso é hipótese, não fato.\n")

    w("## A coluna \"Criança até 12 anos\"\n")
    w("A coluna é mesclada em duas sub-colunas sem título. O esperado era \"quantidade e sexo\" "
      "(\"01 M\" = um menino). A leitura mostrou que **o mesmo campo foi usado de dois jeitos**: "
      "em parte das linhas parece quantidade, em outras é claramente a **idade** da criança.\n")
    w("| Forma da sub-célula | Ocorrências |\n|---|---:|")
    rot = {"VAZIO": "vazia", "XX": "XX", "NUMERO_SEXO": "número + sexo (\"01 M\")",
           "NUMERO": "só número (\"7\")", "OUTRO": "outro texto (\"4 – 7\", \"1 A E 6M\")"}
    for k in ("NUMERO_SEXO", "XX", "VAZIO", "NUMERO", "OUTRO"):
        w(f"| {rot[k]} | {formas.get(k, 0)} |")
    w("")
    w(f"- Famílias em que o valor **só pode ser idade** (número acima de 12, \"anos e meses\", "
      f"duas idades separadas por traço): **{so_idade}**.")
    w(f"- Famílias em que, lido como quantidade, o número de crianças **não cabe no total de "
      f"pessoas** (ex.: \"08 F\" numa família de 2): **{nao_fecha}**.")
    w(f"- Famílias em que a leitura como quantidade é possível e cabe no total: **{propostas}**. "
      "Mesmo nessas, \"01 M\" pode ser um menino ou um menino de 1 ano.")
    if exemplos_outro:
        w(f"- Exemplos de valores fora do padrão (sem identificação): "
          + ", ".join(f"`{e}`" for e in exemplos_outro[:10]) + ".")
    w("")
    w(f"Além disso, as colunas \"Maior de 60\" e \">13<60\" estão vazias em **{pct(adultos_vazios, total)}** "
      "das famílias. Sem elas não dá para conferir se a soma das faixas fecha com o total: só dá "
      "para ver quando passa dele.\n")
    if gestante:
        w(f"A coluna \"Gestante\" está preenchida em {gestante} família(s), com o tempo de gestação "
          "(\"09 MESES\") e sem dizer quem é a gestante. Uma gestação de nove meses anotada na data "
          "do documento já terminou: essa informação vai só para as observações, nunca para o "
          "campo \"gestante\" da pessoa.\n")

    w("## CPF do responsável\n")
    w("O CPF foi conferido pelo dígito verificador, a mesma regra que a API usa. O número não "
      "aparece em lugar nenhum deste relatório.\n")
    w("| Situação | Famílias |\n|---|---:|")
    rot_cpf = {"VALIDO": "válido", "VALIDO_FORMATO_DIFERENTE": "válido, mas escrito fora do padrão (ponto no lugar do traço)",
               "DV_INVALIDO": "11 dígitos, mas dígito verificador errado", "MALFORMADO": "quantidade errada de dígitos",
               "AUSENTE": "em branco", "XX": "\"XXX\""}
    for k in ("VALIDO", "VALIDO_FORMATO_DIFERENTE", "DV_INVALIDO", "MALFORMADO", "XX", "AUSENTE"):
        w(f"| {rot_cpf[k]} | {pct(cpf.get(k, 0), total)} |")
    w(f"| o mesmo CPF em mais de uma linha | {cpf_repetido} |")
    w("")

    w("## Possíveis famílias duplicadas\n")
    w(f"{len(nome_repetido)} linha(s) têm o mesmo nome de responsável que outra linha "
      f"(comparando sem acento, caixa e pontuação); {entre_docs} delas em documentos diferentes. "
      "Nome igual não prova que é a mesma família: a revisão decide (PULAR ou CONFIRMADO).\n")

    w("## Formato B (cadastro por pessoa)\n")
    w("Nenhum dos documentos lidos está no formato por pessoa (com tamanho de roupa e número de "
      "calçado). Por isso não há, nesta rodada, contagem de tamanhos fora da lista fechada nem de "
      "pessoas sem nome: esse formato não existe nos arquivos recebidos e o parser dele não foi "
      "escrito.\n")

    w("## O que precisa de decisão humana\n")
    w("1. **O que \"XX\" significa**, em cada coluna em que aparece.")
    w("2. **O que a coluna \"Criança até 12 anos\" registra**: quantidade por sexo, idade da "
      "criança, ou as duas coisas, dependendo de quem preencheu. Até a resposta, a importação não "
      "atribui sexo a ninguém e guarda o texto original nas observações da família.")
    w("3. **A data de referência de cada documento** (quando a lista foi feita). Os metadados dos "
      "arquivos apontam novembro de 2024 (campanha de Natal), mas metadado não é confirmação.")
    w("4. **O município de cada sítio**, que não aparece em nenhum documento.")
    sem_data = [d["sitio"] for d in docs if comum.ler_data(d["data_referencia"]) is None]
    sem_mun = [d["sitio"] for d in docs if comum.normalizar(texto(d["municipio"])) in comum.DESCONHECIDO]
    if sem_data:
        w(f"\nAinda sem data de referência: {', '.join(sem_data)}.")
    if sem_mun:
        w(f"\nAinda sem município: {', '.join(sem_mun)}.")
    w("")
    return "\n".join(o)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--entrada", type=Path, default=comum.TRABALHO / "extraido.xlsx")
    ap.add_argument("--saida", type=Path, default=comum.TRABALHO / "revisao.xlsx")
    ap.add_argument("--relatorio", type=Path, default=comum.TRABALHO / "relatorio-validacao.md")
    ap.add_argument("--sobrescrever", action="store_true",
                    help="permite gravar por cima de uma revisao.xlsx feita a partir de outra entrada")
    a = ap.parse_args()

    if not a.entrada.is_file():
        print("planilha de entrada não encontrada: rode o extrair.py antes", file=sys.stderr)
        return 2
    if a.saida.exists() and a.saida.resolve() != a.entrada.resolve() and not a.sobrescrever:
        print(f"PAROU: {a.saida.name} já existe e pode ter revisão humana. Rode com "
              f"--entrada {a.saida} para revalidar, ou --sobrescrever para descartá-la.", file=sys.stderr)
        return 3

    docs, linhas, diags = preparar(a.entrada)
    comum.escrever_planilha(a.saida, docs, linhas, diags)
    a.relatorio.write_text(relatorio(docs, linhas, diags), encoding="utf-8")

    erros = sum(1 for d in diags.values() if d.gravidade == ERRO)
    avisos = sum(1 for d in diags.values() if d.gravidade == AVISO)
    print(f"{len(linhas)} família(s) em {len(docs)} documento(s): "
          f"{len(linhas) - erros - avisos} sem ressalva, {avisos} com aviso, {erros} bloqueada(s)")
    por_coluna = Counter(p.coluna for d in diags.values() for p in d.problemas if p.gravidade == ERRO)
    for col, n in por_coluna.most_common():
        print(f"  bloqueio em '{col}': {n}")
    print(f"planilha: {a.saida.name} · relatório: {a.relatorio.name}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
