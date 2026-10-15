#!/usr/bin/env python3
"""Etapa 3 — lê a planilha revisada, monta as famílias e envia para a API.

Uso:
    python enviar.py                 # SIMULA (padrão): valida, monta, não envia nada
    python enviar.py --enviar        # envia de verdade

Credenciais só por variável de ambiente (nunca por argumento, que fica no
histórico do shell):
    IMPORTACAO_API_URL   (padrão http://localhost:3333)
    IMPORTACAO_EMAIL
    IMPORTACAO_SENHA

Na simulação, se as credenciais estiverem definidas, a API é consultada só
para leitura (municípios e comunidades que já existem). Sem elas, a simulação
roda offline.

Pode morrer no meio: POST /api/familias não é idempotente, então cada família
criada vai para trabalho/controle-envio.json (origem -> id) assim que a API
responde, e a próxima execução pula o que já passou. Antes de cada POST a
origem é marcada "em_andamento"; se o processo cair entre o POST e a gravação,
essa família NÃO é reenviada sozinha: confira na tela de Famílias e rode com
--reenviar-pendentes se ela não estiver lá.

O script nunca manda total de pessoas: manda pessoas, e o sistema conta.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sys
from collections import Counter
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path

import requests

import comum
from comum import ERRO, numero_inteiro, origem, texto

TIMEOUT = 60   # a API no Render dorme; a primeira chamada demora


# ---------------------------------------------------------------- controle

class Controle:
    """Arquivo local origem -> id. Tem dado identificável (origem + id): fica em trabalho/, fora do Git."""

    def __init__(self, caminho: Path, api_url: str):
        self.caminho = caminho
        self.dados = {"api_url": api_url, "municipios": {}, "comunidades": {}, "familias": {}}
        if caminho.exists():
            self.dados = json.loads(caminho.read_text(encoding="utf-8"))
            if self.dados.get("api_url") != api_url:
                raise SystemExit(
                    f"PAROU: o arquivo de controle é de outra API ({self.dados.get('api_url')}). "
                    "Use outro --controle para não misturar ids de ambientes diferentes.")

    def salvar(self) -> None:
        self.caminho.parent.mkdir(parents=True, exist_ok=True)
        tmp = self.caminho.with_suffix(".tmp")
        tmp.write_text(json.dumps(self.dados, ensure_ascii=False, indent=1), encoding="utf-8")
        tmp.replace(self.caminho)   # troca atômica: nunca fica meio escrito


# ---------------------------------------------------------------- API

class Api:
    def __init__(self, url: str, email: str, senha: str):
        self.url = url.rstrip("/")
        self._email, self._senha = email, senha
        self.s = requests.Session()
        self.entrar()

    def entrar(self) -> None:
        r = self.s.post(f"{self.url}/api/auth/login", json={"email": self._email, "senha": self._senha},
                        timeout=TIMEOUT)
        if r.status_code != 200:
            raise SystemExit(f"PAROU: login recusado pela API (HTTP {r.status_code}). Confira IMPORTACAO_EMAIL/SENHA.")
        self.s.headers["Authorization"] = f"Bearer {r.json()['accessToken']}"

    def chamar(self, metodo: str, caminho: str, **kw) -> requests.Response:
        r = self.s.request(metodo, f"{self.url}{caminho}", timeout=TIMEOUT, **kw)
        if r.status_code == 401:   # o token de acesso dura 15 min
            self.entrar()
            r = self.s.request(metodo, f"{self.url}{caminho}", timeout=TIMEOUT, **kw)
        return r


def motivo_http(r: requests.Response) -> str:
    """Motivo de falha SEM repassar a mensagem da API: a do 409 de CPF traz o nome do responsável."""
    if r.status_code == 409:
        return "HTTP 409: CPF já cadastrado em outra família na API"
    if r.status_code == 400:
        try:
            msg = r.json().get("message", "")
        except ValueError:
            msg = ""
        campos = sorted(set(re.findall(r"([A-Za-z]+(?:\[\d+\])?(?:\.[A-Za-z]+)*):", msg)))
        return "HTTP 400: recusado pela validação da API" + (f" (campos: {', '.join(campos)})" if campos else "")
    return f"HTTP {r.status_code}"


# ---------------------------------------------------------------- comunidades

@dataclass
class Resolucao:
    municipios: dict[str, str] = field(default_factory=dict)        # chave -> id ou "(novo)"
    comunidades: dict[str, str] = field(default_factory=dict)
    criar_municipios: list[str] = field(default_factory=list)
    criar_comunidades: list[str] = field(default_factory=list)
    bloqueio_comunidade: dict[str, str] = field(default_factory=dict)   # chave -> motivo


def chave_mun(l: dict) -> str:
    return f"{comum.normalizar(texto(l['municipio']))}/{texto(l['uf']).upper()}"


def chave_com(l: dict) -> str:
    return f"{chave_mun(l)}::{comum.chave_comunidade(texto(l['comunidade_nome']))}"


def resolver_lugares(linhas: list[dict], api: Api | None, controle: Controle, enviar: bool) -> Resolucao:
    """Município e comunidade por nome, sem acento e sem caixa. Nome parecido avisa, não mescla."""
    res = Resolucao()

    existentes_mun: dict[str, str] = {}
    if api:
        r = api.chamar("GET", "/api/municipios")
        r.raise_for_status()
        for m in r.json():
            existentes_mun[f"{comum.normalizar(m['nome'])}/{m['uf'].upper()}"] = m["id"]

    for l in linhas:
        k = chave_mun(l)
        if k in res.municipios:
            continue
        if k in controle.dados["municipios"]:
            res.municipios[k] = controle.dados["municipios"][k]
        elif k in existentes_mun:
            res.municipios[k] = controle.dados["municipios"][k] = existentes_mun[k]
        elif enviar:
            r = api.chamar("POST", "/api/municipios",
                           json={"nome": texto(l["municipio"]), "uf": texto(l["uf"]).upper()})
            if r.status_code != 201:
                raise SystemExit(f"PAROU: não foi possível criar o município de {origem(l)} ({motivo_http(r)})")
            res.municipios[k] = controle.dados["municipios"][k] = r.json()["id"]
            res.criar_municipios.append(texto(l["municipio"]))
            controle.salvar()
        else:
            res.municipios[k] = "(novo)"
            res.criar_municipios.append(texto(l["municipio"]))

    # comunidades que já existem na API, por município
    existentes_com: dict[str, dict[str, tuple[str, str]]] = {}   # chave_mun -> {chave_forte: (id, nome)}
    if api:
        for k, mid in res.municipios.items():
            if mid == "(novo)":
                continue
            r = api.chamar("GET", "/api/comunidades", params={"municipioId": mid})
            r.raise_for_status()
            existentes_com[k] = {comum.chave_comunidade(c["nome"]): (c["id"], c["nome"]) for c in r.json()}

    # nomes da própria importação, por município, para pegar "Sítio X" vs "X" entre documentos
    da_importacao: dict[str, set[str]] = {}
    for l in linhas:
        da_importacao.setdefault(chave_mun(l), set()).add(texto(l["comunidade_nome"]))

    for l in linhas:
        k = chave_com(l)
        if k in res.comunidades or k in res.bloqueio_comunidade:
            continue
        km = chave_mun(l)
        nome = texto(l["comunidade_nome"])
        forte = comum.chave_comunidade(nome)
        fraca = comum.chave_comunidade_fraca(nome)
        confirmada = texto(l.get("comunidade_nova_confirmada")).upper() == "SIM"

        if k in controle.dados["comunidades"]:
            res.comunidades[k] = controle.dados["comunidades"][k]
            continue
        existentes = existentes_com.get(km, {})
        if forte in existentes:
            res.comunidades[k] = controle.dados["comunidades"][k] = existentes[forte][0]
            continue

        parecidas = [n for f, (_, n) in existentes.items() if comum.chave_comunidade_fraca(n) == fraca]
        parecidas += [n for n in da_importacao.get(km, set())
                      if n != nome and comum.chave_comunidade_fraca(n) == fraca]
        if parecidas and not confirmada:
            res.bloqueio_comunidade[k] = (
                f"comunidade '{nome}' parecida com '{', '.join(sorted(set(parecidas)))}': "
                "use exatamente o mesmo nome para juntar, ou SIM em comunidade_nova_confirmada")
            continue

        if enviar:
            corpo = {"nome": nome, "municipioId": res.municipios[km]}
            if texto(l.get("comunidade_tipo")):
                corpo["tipoComunidade"] = texto(l["comunidade_tipo"])
            r = api.chamar("POST", "/api/comunidades", json=corpo)
            if r.status_code != 201:
                raise SystemExit(f"PAROU: não foi possível criar a comunidade de {origem(l)} ({motivo_http(r)})")
            res.comunidades[k] = controle.dados["comunidades"][k] = r.json()["id"]
            controle.salvar()
        else:
            res.comunidades[k] = "(novo)"
        res.criar_comunidades.append(nome)
    return res


# ---------------------------------------------------------------- payload

def observacoes(l: dict) -> str:
    """O texto original da linha, sem interpretação. É o que garante que nada se perde."""
    def q(c):
        return f"«{texto(l[c])}»"
    partes = [
        f"Importado do cadastro em Word \"{l['arquivo']}\" (sítio {l['sitio']}, "
        f"família {texto(l['qtd_familia']).rstrip('-').strip()}, documento de {texto(l['data_referencia'])}).",
        f"Texto original: total de pessoas {q('total_pessoas')}; maior de 60 {q('maior_60')}; "
        f">13<60 {q('de_13_a_60')}; criança até 12 anos {q('crianca_ate_12_a')} | {q('crianca_ate_12_b')}; "
        f"gestante {q('gestante')}.",
    ]
    situacao = comum.situacao_cpf(texto(l["cpf"]))
    if situacao in ("AUSENTE", "XX"):
        partes.append(f"CPF não informado no documento ({q('cpf')}).")
    if numero_inteiro(texto(l["total_pessoas"])) is None:
        partes.append("Total de pessoas não informado: só o responsável foi cadastrado.")
    else:
        partes.append("As demais pessoas foram criadas sem nome e sem idade, para completar.")
    return " ".join(partes)


def montar(l: dict, comunidade_id: str, criancas_como_contagem: bool) -> dict:
    total = numero_inteiro(texto(l["total_pessoas"]))
    n = total if total else 1

    responsavel = {
        "nome": " ".join(texto(l["responsavel"]).split()),
        "parentesco": "RESPONSAVEL",
        # o servidor marca incompleto mesmo assim, porque não há idade (RF-09)
        "cadastroIncompleto": False,
    }
    sexos: list[str | None] = []
    if criancas_como_contagem:
        sexos = (["MASCULINO"] * (numero_inteiro(texto(l.get("meninos_ate_12"))) or 0)
                 + ["FEMININO"] * (numero_inteiro(texto(l.get("meninas_ate_12"))) or 0))
    demais = []
    for i in range(n - 1):
        p = {"nome": None, "cadastroIncompleto": True}
        if i < len(sexos):
            p["sexo"] = sexos[i]
        demais.append(p)

    cpf = comum.cpf_digitos(texto(l["cpf"]))
    return {
        "comunidadeId": comunidade_id,
        "responsavelNome": responsavel["nome"],
        "responsavelCpf": cpf if comum.cpf_valido(cpf) else None,
        "pessoas": [responsavel] + demais,
        "fontesRenda": [],
        "observacoes": observacoes(l),
    }


# ---------------------------------------------------------------- execução

def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--planilha", type=Path, default=comum.TRABALHO / "revisao.xlsx")
    ap.add_argument("--controle", type=Path, default=comum.TRABALHO / "controle-envio.json")
    ap.add_argument("--relatorio", type=Path, default=comum.TRABALHO / "relatorio-envio.md")
    ap.add_argument("--enviar", action="store_true", help="envia de verdade (sem isto, só simula)")
    ap.add_argument("--criancas-como-contagem", action="store_true",
                    help="usa meninos_ate_12/meninas_ate_12 para dar sexo às pessoas sem nome. "
                         "Só depois de a associação confirmar que '01 M' é quantidade, não idade")
    ap.add_argument("--reenviar-pendentes", action="store_true",
                    help="reenvia as famílias que ficaram 'em_andamento' (conferir antes na tela de Famílias)")
    a = ap.parse_args()

    if not a.planilha.is_file():
        print("planilha revisada não encontrada: rode extrair.py e validar.py antes", file=sys.stderr)
        return 2
    linhas = comum.ler_aba(a.planilha, comum.ABA_FORMATO_A)
    if not linhas or "comunidade_nome" not in linhas[0]:
        print("a planilha não passou pelo validar.py", file=sys.stderr)
        return 2
    for l in linhas:
        for c in comum.COLUNAS_A:
            l.setdefault(c, None)

    api_url = os.environ.get("IMPORTACAO_API_URL", "http://localhost:3333").rstrip("/")
    email, senha = os.environ.get("IMPORTACAO_EMAIL"), os.environ.get("IMPORTACAO_SENHA")
    if a.enviar and not (email and senha):
        print("PAROU: --enviar exige IMPORTACAO_EMAIL e IMPORTACAO_SENHA no ambiente.", file=sys.stderr)
        return 2
    api = Api(api_url, email, senha) if (email and senha) else None
    controle = Controle(a.controle, api_url)

    modo = "ENVIO" if a.enviar else "SIMULAÇÃO"
    print(f"{modo} · API {api_url if api else '(offline, sem consulta)'}")

    # diagnóstico de novo: a coluna "problemas" da planilha pode estar velha ou editada
    diags = comum.diagnosticar(linhas)

    puladas_revisao, bloqueadas, ja_enviadas, pendentes = [], [], [], []
    candidatas = []
    for i, l in enumerate(linhas):
        chave = comum.chave_origem(l)
        reg = controle.dados["familias"].get(chave)
        if texto(l.get("acao")).upper() == comum.ACAO_PULAR:
            puladas_revisao.append(l)
        elif reg and reg.get("id"):
            ja_enviadas.append(l)
        elif reg and reg.get("estado") == "em_andamento" and not a.reenviar_pendentes:
            pendentes.append(l)
        elif diags[i].gravidade == ERRO:
            bloqueadas.append((l, [p.texto for p in diags[i].problemas if p.gravidade == ERRO]))
        else:
            candidatas.append(l)

    lugares = resolver_lugares(candidatas, api, controle, a.enviar)

    criadas, falhas = [], []
    interrompido = False
    pessoas_criadas = 0
    sexo_atribuido = Counter()
    for l in candidatas:
        k = chave_com(l)
        if k in lugares.bloqueio_comunidade:
            bloqueadas.append((l, [lugares.bloqueio_comunidade[k]]))
            continue
        payload = montar(l, lugares.comunidades[k], a.criancas_como_contagem)
        sexo_atribuido.update(p["sexo"] for p in payload["pessoas"] if p.get("sexo"))
        if not a.enviar:
            criadas.append(l)
            pessoas_criadas += len(payload["pessoas"])
            continue

        chave = comum.chave_origem(l)
        controle.dados["familias"][chave] = {"estado": "em_andamento", "em": datetime.now().isoformat(timespec="seconds")}
        controle.salvar()
        try:
            r = api.chamar("POST", "/api/familias", json=payload)
        except requests.RequestException as e:
            # não sabemos se a família foi criada: fica em_andamento e o laço para aqui
            print(f"PAROU: falha de rede em {origem(l)} ({type(e).__name__}). "
                  "Ela ficou 'em_andamento': confira na tela de Famílias antes de rodar de novo.", file=sys.stderr)
            pendentes.append(l)
            interrompido = True
            break
        if r.status_code == 201:
            corpo = r.json()
            controle.dados["familias"][chave] = {"estado": "criada", "id": corpo["id"],
                                                 "pessoas": len(corpo.get("pessoas", [])),
                                                 "em": datetime.now().isoformat(timespec="seconds")}
            controle.salvar()
            criadas.append(l)
            pessoas_criadas += len(corpo.get("pessoas", []))
            print(f"  criada: {origem(l)}")
        else:
            # a API respondeu que não criou: pode tentar de novo depois de corrigir
            del controle.dados["familias"][chave]
            controle.salvar()
            falhas.append((l, motivo_http(r)))
            print(f"  falhou: {origem(l)} — {motivo_http(r)}")

    # ---------------------------------------------------------- relatório
    verbo = "criadas" if a.enviar else "seriam criadas"
    o = [f"# Importação dos cadastros em Word — {modo.lower()}\n",
         f"{datetime.now().isoformat(timespec='minutes')} · API: "
         f"{api_url if api else 'não consultada (simulação offline)'}\n",
         "| | |", "|---|---:|",
         f"| Linhas na planilha | {len(linhas)} |",
         f"| Famílias {verbo} | {len(criadas)} |",
         f"| Pessoas {verbo} (responsável + sem nome) | {pessoas_criadas} |",
         f"| Pessoas com sexo atribuído pela contagem | {sum(sexo_atribuido.values())} "
         f"{'' if a.criancas_como_contagem else '(desligado: falta --criancas-como-contagem)'} |",
         f"| Municípios {verbo.replace('as', 'os')} | {len(lugares.criar_municipios)} |",
         f"| Comunidades {verbo} | {len(set(lugares.criar_comunidades))} |",
         f"| Puladas: já enviadas antes | {len(ja_enviadas)} |",
         f"| Puladas: PULAR na revisão | {len(puladas_revisao)} |",
         f"| Pendentes de conferência (em_andamento) | {len(pendentes)} |",
         f"| Bloqueadas pela validação | {len(bloqueadas)} |",
         f"| Falharam na API | {len(falhas)} |", ""]
    if lugares.criar_comunidades:
        o.append(f"Comunidades novas: {', '.join(sorted(set(lugares.criar_comunidades)))}.\n")
    motivos = Counter()
    for _, ms in bloqueadas:
        for m in ms:
            motivos[re.sub(r"\(.*?\)", "(…)", m)] += 1
    if motivos:
        o.append("## Motivos de bloqueio\n")
        o.append("| Motivo | Famílias |\n|---|---:|")
        o += [f"| {m} | {n} |" for m, n in motivos.most_common()]
        o.append("")
    if bloqueadas or falhas or pendentes:
        o.append("## Linhas que não entraram (pela origem)\n")
        o += [f"- {origem(l)}: {'; '.join(ms)}" for l, ms in bloqueadas]
        o += [f"- {origem(l)}: falhou — {m}" for l, m in falhas]
        o += [f"- {origem(l)}: em_andamento — conferir na tela de Famílias" for l in pendentes]
        o.append("")
    a.relatorio.parent.mkdir(parents=True, exist_ok=True)
    a.relatorio.write_text("\n".join(o), encoding="utf-8")

    print(f"famílias {verbo}: {len(criadas)} · pessoas: {pessoas_criadas} · "
          f"já enviadas: {len(ja_enviadas)} · PULAR: {len(puladas_revisao)} · "
          f"bloqueadas: {len(bloqueadas)} · falhas: {len(falhas)} · pendentes: {len(pendentes)}")
    print(f"relatório: {a.relatorio.name}")
    return 1 if falhas or interrompido else 0


if __name__ == "__main__":
    sys.exit(main())
