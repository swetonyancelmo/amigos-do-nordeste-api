#!/usr/bin/env python3
"""Etapa 1 — lê um .docx da associação e grava as linhas na planilha, sem interpretar.

Uso (um arquivo por vez, porque município e data mudam por documento):

    python extrair.py "CADASTRO NATAL SÍTIO GILA.docx" \\
        --municipio "Nome do Município" --uf PE --data-referencia 2024-11-28

--municipio e --data-referencia são obrigatórios. Se não forem conhecidos,
passe "desconhecido"/"desconhecida": a extração roda, a validação marca ERRO e
o envio fica bloqueado até alguém perguntar à associação. Nunca se assume hoje.

O que sai: trabalho/extraido.xlsx, com a aba "documentos" (uma linha por
arquivo) e a aba "formato_A" (uma linha por família). Rodar de novo para o
mesmo arquivo substitui as linhas dele.

O console só mostra contagens e a origem (arquivo, sítio, linha): nunca nome nem CPF.
"""
from __future__ import annotations

import argparse
import hashlib
import sys
from datetime import date
from pathlib import Path

import docx
from docx.oxml.ns import qn

import comum
from comum import normalizar


class ForaDoFormato(Exception):
    """Documento ou tabela que não cabe no formato A: para tudo e pergunta."""


def celulas_da_grade(tr) -> list[str]:
    """Texto de cada coluna da GRADE da linha.

    Lê os <w:tc> do XML em vez de row.cells: o python-docx repete o texto de
    célula mesclada na horizontal (gridSpan), e aqui cada coluna da grade
    precisa ser lida uma vez só. Célula mesclada na vertical (vMerge
    continue) não tem texto próprio e é marcada como tal.
    """
    saida = []
    for tc in tr.findall(qn("w:tc")):
        pr = tc.find(qn("w:tcPr"))
        span = 1
        continua = False
        if pr is not None:
            gs = pr.find(qn("w:gridSpan"))
            if gs is not None:
                span = int(gs.get(qn("w:val")))
            vm = pr.find(qn("w:vMerge"))
            continua = vm is not None and vm.get(qn("w:val")) in (None, "continue")
        paragrafos = ["".join(t.text or "" for t in p.iter(qn("w:t"))) for p in tc.findall(qn("w:p"))]
        txt = " ".join(p.strip() for p in paragrafos if p.strip())
        if continua:
            txt = "(mesclada com a linha de cima)"
        saida.append((txt, span))
    return saida


def eh_cabecalho_sitio(tabela) -> bool:
    if len(tabela.rows) == 0:
        return False
    cel = celulas_da_grade(tabela.rows[0]._tr)
    return len(cel) == 2 and normalizar(cel[0][0]).startswith("sitio ou assentamento")


def ler_cabecalho_sitio(tabela) -> dict:
    campos = {}
    for row in tabela.rows:
        cel = celulas_da_grade(row._tr)
        if len(cel) != 2:
            raise ForaDoFormato("tabela de cabeçalho com linha que não tem 2 colunas")
        rotulo = normalizar(cel[0][0])
        valor = cel[1][0]
        if rotulo.startswith("sitio ou assentamento"):
            campos["sitio"] = valor
        elif rotulo.startswith("contato"):
            campos["contato"] = valor
        elif rotulo.startswith("total de familias"):
            campos["total_familias_cabecalho"] = valor
        # o original diz "TOTA DE CRIANÇAS"
        elif rotulo.startswith(("tota de criancas", "total de criancas")):
            campos["total_criancas_cabecalho"] = valor
        else:
            raise ForaDoFormato(f"rótulo de cabeçalho desconhecido: '{cel[0][0]}'")
    return campos


def colunas_formato_a(tabela) -> list[str] | None:
    """Nome da coluna da planilha para cada coluna da grade, ou None se não for formato A."""
    cab = celulas_da_grade(tabela.rows[0]._tr)
    titulos = [normalizar(t).replace(" ", "") if normalizar(t).startswith(">") else normalizar(t) for t, _ in cab]
    if any(k in " ".join(titulos) for k in ("roupa", "calcado", "tamanho", "serie", "nascimento")):
        raise ForaDoFormato(
            "tabela com colunas de roupa/calçado/idade: parece o formato B (por pessoa), "
            "que ainda não tem parser — olhar o arquivo antes de escrever um")
    if not {"responsavel", "total de pessoas", "cpf"} <= set(titulos):
        return None
    grade = []
    for (bruto, span), t in zip(cab, titulos):
        if t not in comum.CABECALHO_A:
            raise ForaDoFormato(f"coluna desconhecida no formato A: '{bruto}'")
        nome = comum.CABECALHO_A[t]
        if nome == "crianca_ate_12":
            if span != 2:
                raise ForaDoFormato("'Criança até 12 anos' deveria ocupar 2 colunas da grade")
            grade += ["crianca_ate_12_a", "crianca_ate_12_b"]
        else:
            if span != 1:
                raise ForaDoFormato(f"coluna '{bruto}' mesclada de um jeito não previsto")
            grade.append(nome)
    return grade


def extrair(caminho: Path, municipio: str, uf: str, data_ref: str) -> tuple[list[dict], list[dict]]:
    d = docx.Document(str(caminho))
    arquivo = caminho.name
    sha = hashlib.sha256(caminho.read_bytes()).hexdigest()
    cp = d.core_properties

    documentos: list[dict] = []
    linhas: list[dict] = []
    cab_atual: dict | None = None
    em_branco = 0

    for ti, tabela in enumerate(d.tables):
        if eh_cabecalho_sitio(tabela):
            cab_atual = ler_cabecalho_sitio(tabela)
            em_branco = 0
            continue
        grade = colunas_formato_a(tabela)
        if grade is None:
            raise ForaDoFormato(f"{arquivo}: tabela {ti} não é cabeçalho de sítio nem tabela de famílias")
        if cab_atual is None:
            raise ForaDoFormato(f"{arquivo}: tabela de famílias {ti} sem cabeçalho de sítio antes dela")
        sitio = cab_atual.get("sitio", "")
        familias_tabela = 0
        for ri, row in enumerate(tabela.rows[1:], start=1):
            cel = celulas_da_grade(row._tr)
            textos = [t for t, s in cel for _ in range(s)]
            if len(textos) != len(grade):
                raise ForaDoFormato(f"{arquivo} · tabela {ti} · linha {ri}: {len(textos)} colunas, esperadas {len(grade)}")
            valores = dict(zip(grade, textos))
            resto = [v for k, v in valores.items() if k != "qtd_familia"]
            if not any(v.strip() for v in resto):
                if valores["qtd_familia"].strip():
                    em_branco += 1   # linha numerada à espera de alguém
                continue
            familias_tabela += 1
            linhas.append({
                "arquivo": arquivo, "sitio": sitio, "tabela": ti, "linha": ri,
                "municipio": municipio, "uf": uf, "data_referencia": data_ref,
                **valores,
            })
        documentos.append({
            "arquivo": arquivo, "sha256": sha, "formato": "A", "tabelas": len(d.tables),
            "sitio": sitio,
            "contato": "(preenchido)" if cab_atual.get("contato", "").strip() else "",
            "total_familias_cabecalho": cab_atual.get("total_familias_cabecalho", ""),
            "total_criancas_cabecalho": cab_atual.get("total_criancas_cabecalho", ""),
            "linhas_numeradas_em_branco": em_branco,
            "municipio": municipio, "uf": uf, "data_referencia": data_ref,
            "criado_em_metadado": cp.created.date().isoformat() if cp.created else "",
            "modificado_em_metadado": cp.modified.date().isoformat() if cp.modified else "",
        })
        cab_atual = None

    if not documentos:
        raise ForaDoFormato(f"{arquivo}: nenhuma tabela de famílias encontrada")
    return documentos, linhas


def validar_data(valor: str) -> str:
    if normalizar(valor) in comum.DESCONHECIDO:
        return "desconhecida"
    try:
        d = date.fromisoformat(valor)
    except ValueError:
        raise argparse.ArgumentTypeError("use AAAA-MM-DD ou 'desconhecida'")
    if d > date.today():
        raise argparse.ArgumentTypeError("data no futuro")
    return d.isoformat()


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("arquivo", type=Path)
    ap.add_argument("--municipio", required=True, help="município do documento (não aparece no .docx)")
    ap.add_argument("--uf", required=True)
    ap.add_argument("--data-referencia", required=True, type=validar_data,
                    help="data em que o documento foi feito (AAAA-MM-DD) ou 'desconhecida'")
    ap.add_argument("--saida", type=Path, default=comum.TRABALHO / "extraido.xlsx")
    ap.add_argument("--substituir", action="store_true",
                    help="permite trocar linhas deste arquivo que já passaram por revisão")
    a = ap.parse_args()

    if not a.arquivo.is_file():
        print(f"arquivo não encontrado: {a.arquivo.name}", file=sys.stderr)
        return 2
    municipio = "desconhecido" if normalizar(a.municipio) in comum.DESCONHECIDO else a.municipio.strip()

    try:
        documentos, linhas = extrair(a.arquivo, municipio, a.uf.strip().upper(), a.data_referencia)
    except ForaDoFormato as e:
        print(f"PAROU: {e}\nNada foi gravado. Descreva o documento e pergunte antes de adaptar o script.",
              file=sys.stderr)
        return 3

    docs_ant, linhas_ant = [], []
    if a.saida.exists():
        docs_ant = comum.ler_aba(a.saida, comum.ABA_DOCUMENTOS)
        linhas_ant = comum.ler_aba(a.saida, comum.ABA_FORMATO_A)
    sha = documentos[0]["sha256"]
    for doc in docs_ant:
        if doc["sha256"] == sha and doc["arquivo"] != a.arquivo.name:
            print(f"PAROU: conteúdo idêntico a '{doc['arquivo']}', que já foi extraído (arquivo duplicado).",
                  file=sys.stderr)
            return 4
    revisadas = [l for l in linhas_ant if l["arquivo"] == a.arquivo.name
                 and any(comum.texto(l.get(c)) for c in comum.COLUNAS_REVISAO + comum.COLUNAS_DIAGNOSTICO)]
    if revisadas and not a.substituir:
        print(f"PAROU: {len(revisadas)} linha(s) deste arquivo já têm revisão na planilha de saída. "
              "Use --substituir se quiser mesmo extrair de novo.", file=sys.stderr)
        return 5

    docs = [d for d in docs_ant if d["arquivo"] != a.arquivo.name] + documentos
    todas = [l for l in linhas_ant if l["arquivo"] != a.arquivo.name] + linhas
    comum.escrever_planilha(a.saida, docs, todas)

    for doc in documentos:
        n = sum(1 for l in linhas if l["sitio"] == doc["sitio"])
        print(f"{a.arquivo.name}: formato A · sítio '{doc['sitio']}' · {n} família(s) · "
              f"{doc['linhas_numeradas_em_branco']} linha(s) numerada(s) em branco")
    print(f"gravado em {a.saida.relative_to(comum.PASTA) if a.saida.is_relative_to(comum.PASTA) else a.saida}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
