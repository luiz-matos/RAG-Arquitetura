# RAG-Arquitetura

Sistema de RAG (Retrieval-Augmented Generation) que responde, em português, perguntas sobre
arquitetura de software, usando como fonte a série de livros *The Architecture of Open Source
Applications*. Projeto de estudo e portfólio, acompanhado de um artigo.

> Estado atual: livro baixado, infraestrutura (banco e modelos) no ar e testada, e o
> esqueleto do projeto Java criado. O RAG em si ainda não foi implementado.

## Objetivo

Construir um RAG simples, mas bem feito, e medir quanto cada técnica melhora a resposta.

## Pesquisa: construção por etapas

O sistema começa numa versão base e recebe uma técnica por vez. Cada etapa é medida contra a
anterior, para mostrar quanto aquela peça contribuiu.

| Etapa | O que muda | Pergunta que responde |
|---|---|---|
| 1. Base | Chunking de tamanho fixo + busca só vetorial | Ponto de partida para comparação |
| 2. Chunking por seção | Corta o livro por seção, não por tamanho | Respeitar a estrutura do livro melhora a recuperação? |
| 3. Busca híbrida | Vetorial + palavra-chave | Termos exatos (ex.: "NameNode", "Dialplan") passam a ser encontrados? |
| 4. Tradução da pergunta | Pergunta em português é traduzida para inglês antes da busca | Traduzir a pergunta supera buscar direto em português? |
| 5. Reranking | Reordena os trechos recuperados antes de gerar a resposta | Reordenar compensa um modelo de geração pequeno? |

### Avaliação: a prova

Cada etapa responde à mesma lista de perguntas de referência. Cada pergunta anota onde está a
resposta certa no livro:

| Pergunta | Resposta está em |
|---|---|
| O que é um channel no Asterisk? | `v1/asterisk`, seção 1.1.1 Channels |
| Como o nginx atende muitas conexões ao mesmo tempo? | `v2/nginx` |
| Como o HDFS evita perder dados quando um disco falha? | `v1/hdfs` |

**Nota da etapa:** em quantas perguntas o trecho certo apareceu entre os 5 primeiros
recuperados. A etapa é comparada com a anterior pela nota.

## Fonte dos dados

Série **The Architecture of Open Source Applications** (aosabook.org), os 4 livros:

| Livro | Pasta no site | Capítulos | Conteúdo |
|---|---|---|---|
| AOSA Volume 1 | `v1` | 25 | Arquitetura de sistemas reais |
| AOSA Volume 2 | `v2` | 24 | Arquitetura de sistemas reais |
| The Performance of Open Source Applications | `posa` | 12 | Foco em performance |
| 500 Lines or Less | `500L` | 22 | Sistemas pequenos, com bastante código |

| Motivo | Detalhe |
|---|---|
| Licença aberta | Creative Commons Attribution 3.0: permite usar e publicar, com atribuição aos autores |
| Repositório reproduzível | Quem avaliar o projeto consegue baixar a mesma fonte |
| Conteúdo adequado | Cada capítulo descreve a arquitetura de um sistema real |
| Estrutura clara | Capítulos e seções bem definidos, o que viabiliza o chunking por seção |

Livros comerciais de arquitetura ficaram de fora: não podem ser publicados num repositório
público, nem o texto nem o índice gerado a partir dele.

### Download

O livro é publicado em HTML, com capítulo em `<h1>`, seção em `<h2>` e subseção em `<h3>`. Essa
estrutura é o que viabiliza o chunking por seção.

```bash
python scripts/baixar_aosa.py
```

| Decisão | Motivo |
|---|---|
| Script em Python, só com a biblioteca padrão | Roda sem instalar nada; é executado uma vez, fora do sistema |
| O script só baixa o HTML cru | Limpar e cortar fica na ingestão em Java; mudar o chunking não exige baixar de novo |
| HTML baixado fora do Git (`data/raw/` no `.gitignore`) | Repositório leve; o script reproduz o download |
| `data/raw/manifesto.json` | Registra de onde veio cada página e quando, para citar a fonte na resposta |
| Pausa de 1 segundo entre páginas | Não sobrecarregar o site |
| Página já baixada é pulada | Se falhar no meio, rodar de novo baixa só o que falta |

São 89 páginas, uns 4 MB. Nem todas entram no banco:

| Tipo | Quantas | Entra no banco | Motivo |
|---|---|---|---|
| Capítulos | 83 | Sim | O livro em si |
| Introduções | 4 | Sim | Apresentação e biografia dos autores; respondem "quem escreveu o capítulo X?" |
| Bibliografias (`bib1`, `bib2`) | 2 | Não | Só listas de títulos de artigos; atrapalham a busca e não respondem nada |

## Tecnologias

| Tecnologia | Papel | Por que |
|---|---|---|
| Java 25 | Linguagem do sistema | Tipagem deixa os contratos da arquitetura explícitos; linguagem que domino. Sempre a LTS mais nova: suporte longo e é nela que as bibliotecas testam primeiro |
| Spring Boot 4.x | Base da aplicação | Versão mais nova; configuração, injeção de dependência e API prontas |
| LangChain4j | Biblioteca de RAG | Traz recuperação, busca híbrida e reranking prontos, mais do que o Spring AI |
| PostgreSQL + pgvector | Banco de dados | Guarda texto, metadados e vetores num banco só; busca híbrida nativa (pgvector + texto completo do PostgreSQL); SQL já conhecido |
| Ollama | Execução do LLM local | Roda o modelo na própria máquina, sem custo de API e sem enviar dados para fora |
| Docker Compose | Ambiente | Sobe banco e modelo com um comando, igual em qualquer máquina |

Alternativas descartadas:

| Alternativa | Por que não |
|---|---|
| Python | Ecossistema maior para RAG, mas o foco aqui é a arquitetura, e Java mostra melhor os contratos |
| Spring AI | Menos recursos de RAG prontos que o LangChain4j |
| Qdrant ou Chroma | Banco só de vetor; faria sentido se o foco fosse escala, não é o caso |
| LLM por API | Custo por chamada e dado saindo da máquina |

### Modelos

Todos rodam na CPU (ver [Ambiente de desenvolvimento](#ambiente-de-desenvolvimento)).

| Papel | Modelo | Alternativa | Por que |
|---|---|---|---|
| Geração: escreve a resposta e traduz a pergunta | `qwen3:4b-instruct` | Gemma 3 4B | Bom em português e leve |
| Embedding: transforma texto em vetor para a busca | `nomic-embed-text` (768 dimensões) | bge-m3 | Pequeno e bom em inglês; a pergunta chega traduzida, então não precisa ser multilíngue |
| Reranking: dá nota de relevância a cada trecho | ms-marco-MiniLM | bge-reranker-base | Muito pequeno, roda dentro da aplicação Java |

Fluxo de uma pergunta:

1. A geração traduz a pergunta para inglês.
2. O embedding busca os 20 trechos mais próximos.
3. O reranking escolhe os 5 melhores.
4. A geração escreve a resposta em português.

**Por que a versão `instruct`:** o `qwen3:4b` padrão pensa antes de responder e ignora o
pedido para desligar isso. Na CPU, gerou mais de 1.500 tokens de raciocínio para traduzir uma
frase, uns 5 minutos. O `instruct` responde direto:

| Teste: traduzir uma pergunta | `qwen3:4b` | `qwen3:4b-instruct` |
|---|---|---|
| Tokens gerados | Mais de 1.500 (cancelado) | 12 |
| Tempo | Mais de 5 minutos | 7 segundos (4,6 s carregando o modelo) |
| Velocidade | 5,3 tokens/s | 7,1 tokens/s |

## Decisões de arquitetura

### Dois fluxos separados

| Fluxo | Quando roda | O que faz |
|---|---|---|
| Ingestão | Uma vez por versão do índice | Lê o livro, corta em trechos, gera os vetores e grava no banco |
| Consulta | A cada pergunta | Traduz a pergunta, busca os trechos, reordena e gera a resposta |

### Modelo de embedding gravado junto com o vetor

Cada trecho guarda o nome do modelo que gerou o vetor. Trocar de modelo exige reindexar tudo,
e isso impede misturar vetores incompatíveis na mesma busca.

### Tradução: só a pergunta

O livro fica em inglês no banco. A pergunta em português é traduzida para inglês antes da
busca, e o modelo gera a resposta direto em português.

| Opção | Custo | Problema | Decisão |
|---|---|---|---|
| Traduzir o livro antes de gravar | Horas de processamento | Erro de tradução fica gravado; citação não bate com o original | Descartada |
| Traduzir a pergunta e a resposta de volta | 2 chamadas extras ao modelo por pergunta | Mais lento | Alternativa, se o português sair ruim |
| Traduzir a pergunta, resposta direto em português | 1 chamada extra | Modelo pequeno escreve português pior que inglês | **Escolhida** |

Motivo principal: a parte por palavra-chave da busca híbrida só funciona se a pergunta e o
texto estiverem na mesma língua.

### Reranking com modelo pequeno dentro do Java

Reordenar os trechos com o próprio LLM exige uma chamada por trecho, inviável sem GPU. O
reranking usa um modelo pequeno próprio para isso, executado dentro da aplicação pelo
LangChain4j.

## Ambiente de desenvolvimento

| Peça | Valor | Consequência |
|---|---|---|
| CPU | Intel i5-12400F | Todo o processamento do modelo roda aqui |
| RAM | 16 GB | Modelos de 3B rodam bem; 7B a 8B rodam, mas devagar |
| GPU | NVIDIA GT 420 (2 GB) | Antiga demais para o Ollama, não é usada |

### Docker Compose

O [docker-compose.yml](docker-compose.yml) sobe a infraestrutura. A aplicação Java roda fora
do Docker e acessa os serviços pelas portas.

```bash
docker compose up -d
```

| Serviço | O que faz | Porta |
|---|---|---|
| `postgres` | PostgreSQL 18 com pgvector (banco `rag`, usuário `rag`, senha `rag`, só local) | 5433 |
| `ollama` | Servidor que roda os modelos | 11434 |
| `ollama-modelos` | Baixa os dois modelos ao subir o ambiente e termina; modelo já baixado é pulado | - |

| Decisão | Motivo |
|---|---|
| Porta 5433 para o PostgreSQL | A 5432 já é usada pelo PostgreSQL instalado no Windows |
| Volumes para banco e modelos | Os dados sobrevivem a `docker compose down` |
| `restart: on-failure:20` no `ollama-modelos` | A conexão com o servidor dos modelos cai com frequência; o Docker tenta de novo sozinho |
| `OLLAMA_NOPRUNE` | Sem isso, o Ollama apaga o download incompleto ao reiniciar e tudo recomeça do zero |
| `up -d` em vez de `start` | O `start` só religa containers que já existem; não cria serviço novo |

### Build

Precisa do JDK 25. O Maven vem pelo wrapper (`mvnw`), sem instalar nada. Se o JDK 25 não for
o padrão da máquina, aponte o `JAVA_HOME` para ele antes do build:

```bash
./mvnw verify
```

O `verify` compila, confere a formatação do código (Spotless, padrão Palantir) e roda os
testes. Para corrigir a formatação automaticamente: `./mvnw spotless:apply`.

## Licença

| Parte | Licença |
|---|---|
| Código deste projeto | MIT ([LICENSE](LICENSE)) |
| Texto do livro (baixado pelo script, não versionado) | Creative Commons Attribution 3.0 Unported, dos autores originais |

## Pendências

1. Montar a lista completa de perguntas de referência (a prova).
