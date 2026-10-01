"""Baixa o HTML cru dos 4 livros do site aosabook.org.

O script só baixa: não limpa, não corta e não traduz. O processamento fica na
ingestão em Java, para que mudar o chunking não exija baixar tudo de novo.

Uso (na raiz do projeto):
    python scripts/baixar_aosa.py

Saída:
    data/raw/<livro>/<capitulo>.html   um arquivo por página do livro
    data/raw/manifesto.json            lista do que foi baixado, de onde e quando
"""

import json
import re
import sys
import time
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urljoin
from urllib.request import Request, urlopen

URL_BASE = "https://aosabook.org/en/"

# Pasta de cada livro no site.
LIVROS = {
    "v1": "The Architecture of Open Source Applications, Volume 1",
    "v2": "The Architecture of Open Source Applications, Volume 2",
    "posa": "The Performance of Open Source Applications",
    "500L": "500 Lines or Less",
}

PASTA_SAIDA = Path(__file__).resolve().parent.parent / "data" / "raw"

# Pausa entre downloads, para não sobrecarregar o site.
PAUSA_SEGUNDOS = 1.0

CABECALHOS = {"User-Agent": "RAG-Arquitetura/estudo (download do livro sob CC BY 3.0)"}


def baixar(url: str) -> bytes:
    with urlopen(Request(url, headers=CABECALHOS), timeout=30) as resposta:
        return resposta.read()


def mapear_paginas(html_indice: str) -> list[str]:
    """Lê a página inicial e devolve os caminhos das páginas dos 4 livros.

    Ex.: "v1/asterisk.html". Links com âncora (#autor) apontam para a mesma
    página, então a âncora é descartada e a lista não tem repetição. O índice
    usa aspas duplas em alguns links e simples em outros (os do 500L).
    """
    pastas = "|".join(re.escape(p) for p in LIVROS)
    padrao = re.compile(rf"""href=["']((?:{pastas})/[^"'#]+\.html)""")
    caminhos = {c for c in padrao.findall(html_indice) if not c.endswith("/index.html")}
    return sorted(caminhos)


def main() -> int:
    print(f"Lendo o índice: {URL_BASE}")
    caminhos = mapear_paginas(baixar(URL_BASE).decode("utf-8"))
    print(f"{len(caminhos)} páginas encontradas")

    manifesto = []
    falhas = []

    for i, caminho in enumerate(caminhos, start=1):
        url = urljoin(URL_BASE, caminho)
        destino = PASTA_SAIDA / caminho
        livro = caminho.split("/")[0]

        # Arquivo já baixado numa execução anterior não é baixado de novo.
        if destino.exists():
            print(f"[{i}/{len(caminhos)}] já existe: {caminho}")
        else:
            try:
                conteudo = baixar(url)
            except Exception as erro:
                print(f"[{i}/{len(caminhos)}] FALHOU: {caminho} ({erro})")
                falhas.append(caminho)
                continue
            destino.parent.mkdir(parents=True, exist_ok=True)
            destino.write_bytes(conteudo)
            print(f"[{i}/{len(caminhos)}] baixado: {caminho}")
            time.sleep(PAUSA_SEGUNDOS)

        manifesto.append({
            "livro": LIVROS[livro],
            "pasta": livro,
            "arquivo": caminho,
            "url": url,
            "baixado_em": datetime.fromtimestamp(destino.stat().st_mtime, timezone.utc).isoformat(),
        })

    PASTA_SAIDA.mkdir(parents=True, exist_ok=True)
    (PASTA_SAIDA / "manifesto.json").write_text(
        json.dumps(
            {
                "fonte": URL_BASE,
                "licenca": "Creative Commons Attribution 3.0 Unported",
                "paginas": manifesto,
            },
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )

    print(f"\nConcluído: {len(manifesto)} páginas em {PASTA_SAIDA}")
    if falhas:
        print(f"{len(falhas)} falhas. Rode de novo para tentar só as que faltam: {falhas}")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
