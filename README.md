# SRM Credit Engine

Plataforma de cessão de crédito multimoedas: recebe lotes de recebíveis, aplica o deságio conforme o
risco do ativo, converte para a moeda de pagamento e registra a liquidação de forma auditável.

Desafio técnico da SRM Asset, entregue no nível **Pleno**.

---

## Como rodar

### Tudo em containers

Único pré-requisito é Docker.

```bash
docker compose up -d --build
```

| Serviço | Endereço |
|---|---|
| Interface | http://localhost:3000 |
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| PostgreSQL | localhost:5432 (`srm` / `srm` / base `srm_credit`) |

O schema e os dados de referência (moedas, tipos de recebível com os spreads do enunciado, cotações
iniciais e dois cedentes) são aplicados pelo Flyway na subida. A aplicação sobe pronta para operar.

### Desenvolvimento local

Banco em container, aplicação e interface na máquina:

```bash
docker compose up -d db

cd backend && ./mvnw spring-boot:run     # http://localhost:8080
cd frontend && npm install && npm run dev # http://localhost:5173
```

Testes do backend: `cd backend && ./mvnw test` — 63 testes.
Verificação estática do frontend: `cd frontend && npx tsc -b && npm run build && npm run lint`.

> **Atenção ao rebuild.** A interface é servida como bundle estático, montado em tempo de build da
> imagem. `docker compose up -d` sem `--build` sobe o container com o bundle antigo, mesmo que o
> código-fonte tenha mudado. Depois de mexer no frontend, use `docker compose up -d --build web`.

### Conferindo a interface

Roteiro que exercita as regras de negócio de ponta a ponta, em cerca de dois minutos:

**Painel do operador** (http://localhost:3000/painel)

1. Digite `10000` no valor de face de uma duplicata com vencimento em 60 dias. O valor presente
   aparece sozinho: **R$ 9.518,14**, que é `10.000 / 1,025²`. O deságio de R$ 481,86 é o que o fundo
   ganha na operação.
2. Troque o tipo para **cheque pré-datado**. O valor presente cai, porque o spread sobe de 1,5% para
   2,5% ao mês. Mais risco, mais deságio.
3. Mude o vencimento para 45 dias. O prazo vira 1,5 mês comercial e o valor sobe para **R$ 9.636,39** —
   note que não é a média entre 30 e 60 dias, porque o desconto é composto.
4. Troque a moeda de pagamento para **USD**. O líquido passa a **US$ 1.756,10**, com a cotação
   aplicada visível abaixo do valor. A conversão entra no fim, sobre o valor presente.
5. Preencha a referência e o número do documento, e clique em **Registrar operação**. A operação nasce
   **Pendente**.
6. Clique em **Liquidar agora** — vira **Liquidada**. Clique em **Novo lote** e tente registrar de novo
   com a mesma referência: o `409` aparece no topo, porque referência é chave de idempotência.

**Extrato de liquidação** (http://localhost:3000/transacoes)

7. Os totais no topo vêm separados por par de moedas — somar BRL com USD numa cifra só não
   significaria nada.
8. Filtre por moeda ou cedente. Repare que **a URL muda junto**: o extrato filtrado é um link
   compartilhável, e o botão voltar desfaz o filtro.
9. Ordene por **Líquido**. Aparece um aviso de que a ordenação está comparando moedas diferentes.
   Aplique o filtro de moeda e o aviso some.
10. As operações pendentes ficam no fim da lista, com `—` na data de liquidação.

**Casos de erro que valem ver**

- Valor de face negativo: o erro aparece no campo, não num alerta genérico.
- Vencimento anterior à emissão: o campo é marcado antes de qualquer chamada à API.
- Título já vencido: a API recusa com `422` e a mensagem explica qual documento.

---

## Escolha da stack

**Java 21 + Spring Boot 3.5.** O requisito é um sistema financeiro com precisão decimal e integridade
transacional. `BigDecimal`, `@Transactional` com semântica clara, lock otimista via JPA e um
ecossistema maduro de validação resolvem exatamente esse problema. Tipagem forte importa aqui menos
por gosto e mais porque um erro de tipo em cálculo monetário é um erro de dinheiro.

**PostgreSQL + Flyway.** `NUMERIC` de precisão arbitrária, `CHECK` constraints que sustentam as
invariantes financeiras no próprio banco, e migrações versionadas e imutáveis — schema de sistema
financeiro precisa de histórico auditável tanto quanto os dados.

**`ch.obermuhlner:big-math`.** `BigDecimal.pow` só aceita expoente inteiro. O prazo em meses
comerciais é fracionário (45 dias = 1,5 mês), então a potenciação precisa de uma biblioteca de
precisão arbitrária. Arredondar o prazo para meses inteiros distorceria o deságio de toda operação
que não caísse em múltiplo de 30 dias.

**React + Vite + TypeScript + TanStack Query.** Sem Redux: quase todo o estado desta aplicação é
estado de servidor — cotações, cadastro, extrato. Isso é cache, não estado global, e precisa de
invalidação, refetch e deduplicação, que é o que o Query resolve e o que um store manual sempre
reimplementa mal.

---

## Arquitetura

Três camadas, com dependência fluindo em uma direção só:

```
api  ──►  domain  ──►  infrastructure
(HTTP)    (regras)     (persistência, integrações)
```

- **`api`** — controllers, DTOs, validação de entrada, tradução de exceção para status HTTP.
- **`domain`** — entidades ricas e serviços de negócio. Não conhece HTTP.
- **`infrastructure`** — repositórios Spring Data, leitor de SQL nativo, provedor externo de cotações.

**Relatórios usam duas camadas**, conforme o item 6 do enunciado autoriza: o controller do extrato
fala direto com o `SettlementReportReader`, sem serviço de negócio no meio, porque não há regra a
aplicar — só um recorte de dados.

Diagrama ER, DDL consolidado e as decisões de modelagem estão em
[`docs/modelo-de-dados.md`](docs/modelo-de-dados.md).

---

## Regras de negócio

### Precificação

```
Valor Presente = Valor de Face / (1 + Taxa Base + Spread) ^ Prazo em meses
```

- **Prazo em meses comerciais de 30 dias, com expoente fracionário.** 45 dias valem 1,5 mês.
- **O prazo conta da data de referência até o vencimento**, não da emissão: o que desconta é quanto
  tempo o fundo ainda vai esperar pelo dinheiro.
- **Strategy por tipo de recebível.** `PricingStrategy` devolve a taxa mensal de desconto; o motor
  não sabe como cada tipo chegou nela. O spread vive no cadastro, não no código, para a mesa ajustar
  risco sem deploy.
- **A aplicação se recusa a subir** se existir tipo de recebível ativo sem estratégia — um buraco
  desses só apareceria na primeira operação que usasse o tipo.

### Câmbio

- **Cotação é histórico, não estado.** Registrar não sobrescreve: insere. "Vigente" é a de
  `effective_at` mais recente que já passou, o que faz cotação com vigência futura funcionar de graça.
- **Sem inversão implícita de par.** Pedir USD/BRL não usa `1/(BRL/USD)` — cada ponta tem spread
  próprio, e inverter escondido produziria uma taxa que ninguém registrou.
- **A conversão entra no fim**, sobre o valor presente já arredondado, que é a cifra que vai para o
  banco em `NUMERIC(18,2)`.
- **`HALF_EVEN` em todo arredondamento monetário.** `HALF_UP` empurra todo empate para cima e, em
  volume, vira viés de alta sistemático no caixa.

### Liquidação

- **O lote é o agregado.** Cabeçalho e títulos entram na mesma transação: ou o lote inteiro é
  gravado, ou nada é. Liquidação parcial deixa de ser representável no banco.
- **Registrar e liquidar são passos distintos.** O preço trava no registro; a liquidação move o
  dinheiro. É no `PENDING` que a operação pode ser conferida.
- **As invariantes moram na entidade.** `settle()` e `cancel()` são os únicos caminhos de transição —
  não existe setter de status.
- **Concorrência por lock otimista.** Duas chamadas simultâneas de liquidação disputam o mesmo
  `UPDATE`; a perdedora recebe `409` em vez de liquidar duas vezes.
- **A referência é chave de idempotência.** `UNIQUE` no banco: reenviar o mesmo lote não cria uma
  segunda operação.

### Totais

O total do lote é a **soma dos títulos já arredondados**, não o arredondamento da soma. É o único
jeito de o cabeçalho fechar com o linha a linha do extrato, que é o que a auditoria confere.

---

## API

Documentação interativa em `/swagger-ui.html`. Erros seguem RFC 7807
(`application/problem+json`) com as extensões `code`, `timestamp` e, nas falhas de validação,
`violations`.

| Verbo | Rota | Uso |
|---|---|---|
| `GET` | `/api/currencies` | moedas suportadas |
| `GET` | `/api/receivable-types` | tipos de recebível e spreads |
| `GET` | `/api/assignors` | cedentes cadastrados |
| `POST` | `/api/exchange-rates` | registra cotação manual |
| `POST` | `/api/exchange-rates/sync` | importa do provedor simulado |
| `GET` | `/api/exchange-rates/current` | cotação vigente de um par |
| `GET` | `/api/exchange-rates` | histórico de cotações |
| `POST` | `/api/pricing/simulate` | precifica um lote, sem persistir |
| `POST` | `/api/settlements` | registra lote → `PENDING` |
| `GET` | `/api/settlements/{referência}` | consulta a operação |
| `POST` | `/api/settlements/{referência}/settle` | liquida |
| `POST` | `/api/settlements/{referência}/cancel` | cancela |
| `GET` | `/api/reports/settlements` | extrato paginado |
| `GET` | `/api/reports/settlements/summary` | totais por par de moedas |

### Mapeamento de erros

| Situação | Status | `code` |
|---|---|---|
| Recurso inexistente | 404 | `RESOURCE_NOT_FOUND` |
| Validação de entrada | 400 | `VALIDATION_FAILED` |
| Regra de negócio recusou | 422 | código da regra |
| Estado conflitante / concorrência / constraint | 409 | `SETTLEMENT_NOT_PENDING`, `CONCURRENT_MODIFICATION`, `DATA_INTEGRITY_VIOLATION` |
| Falha inesperada | 500 | `INTERNAL_ERROR` + `incident` |

**422 para regra de negócio e 400 para validação** é deliberado: o payload sintaticamente correto que
a regra rejeita não é o mesmo erro de um campo mal preenchido. Separar diz ao cliente se vale corrigir
o campo ou se a operação simplesmente não é permitida.

**500 nunca vaza detalhe interno.** A mensagem vai para o log com um UUID de incidente; o cliente
recebe só o UUID.

---

## Interface

**Painel do operador** — entrada do lote com simulação em tempo real. A simulação chama a API a cada
mudança (com debounce), porque o navegador não faz conta de dinheiro: fórmula, câmbio e arredondamento
ficam todos no servidor.

**Extrato de liquidação** — paginação server-side, filtros por período, cedente, moeda e status,
ordenação por colunas e totais consolidados por par de moedas. O recorte vive na query string, então
um extrato filtrado é um link compartilhável e o botão voltar desfaz o filtro anterior.

A separação entre apresentação e estado é estrutural: os hooks concentram estado e montagem de
payload, e os componentes só renderizam props — nenhum deles importa o cliente HTTP.

---

## Critérios de aceite

### Usabilidade
- O valor líquido aparece sem o operador pedir, a cada alteração do lote.
- Erro de validação aparece no campo que o causou, incluindo a posição no lote (`items[1].faceValue`).
- Erro que o operador não resolve no input (conflito, indisponibilidade) sobe como aviso no topo.
- Enquanto o próximo cálculo chega, os números anteriores ficam visíveis e esmaecidos, nunca em branco.
- Um extrato filtrado é compartilhável por link e sobrevive a um F5.

### Segurança
- Toda entrada é validada antes de chegar ao domínio; campo desconhecido no payload é rejeitado.
- A única parte do SQL montada por concatenação é a ordenação, e o valor vem de um `enum` — nome de
  coluna arbitrário não tem caminho até a query. Todo o resto usa parâmetros nomeados.
- Resposta de erro nunca expõe stack trace, SQL, nome de constraint ou string de conexão.
- CORS restrito por origem configurável, sem curinga.
- Container da API roda com usuário sem privilégio.

### Desempenho
- Extrato paginado no servidor; o cliente nunca recebe a tabela inteira.
- Consultas do extrato apoiadas nos índices compostos de `settled_at` — confirmado com `EXPLAIN`.
- Relatório em SQL nativo com projeção das colunas da tela, sem hidratar agregados pelo ORM.
- Total da página vem por função de janela na mesma consulta, não numa segunda ida ao banco.
- Cotação resolvida uma vez por lote, não uma vez por título.

### Escalabilidade
- Aplicação sem estado em memória entre requisições: replicar é subir mais instâncias.
- Concorrência resolvida por lock otimista, que não serializa leituras.
- Idempotência por referência permite retry de cliente sem duplicar operação.
- Cadastro (moedas, tipos, cedentes) tem cache no cliente, tirando carga de leitura repetitiva.

---

## Git

`main` protegida por convenção: todo trabalho passou por branch e Pull Request, mesmo em equipe de um.
Quinze PRs, cinquenta commits atômicos em Conventional Commits e merges por rebase — o histórico é
linear, sem um único commit de merge poluindo a leitura.

Cada PR descreve o que foi feito, por que cada decisão foi tomada e como foi verificada, incluindo os
defeitos encontrados durante a verificação.

---

## Checklist do enunciado

| Requisito | Onde está |
|---|---|
| **1.** Currency Engine: armazenar e prover taxas | `ExchangeRateService`, histórico append-only com vigência |
| **1.** Endpoint de atualização manual **ou** integração mockada | Os dois: `POST /api/exchange-rates` e `POST /api/exchange-rates/sync` |
| **2.** Strategy por tipo de recebível | `PricingStrategy` + registro com verificação de cobertura na subida |
| **2.** Fórmula do valor presente | `PricingService.presentValue`, expoente fracionário via big-math |
| **2.** Conversão cambial no final | `PricedBatch`, sobre o valor presente já arredondado |
| **3.** Banco relacional | PostgreSQL 16 + Flyway |
| **3.** ACID, sem liquidação pela metade | Lote como agregado numa transação + `CHECK` constraints |
| **3.** Race conditions | Lock otimista por `@Version`, testado com 6 chamadas simultâneas |
| **4.** API REST com verbos e status semânticos | 14 rotas, mapeamento de erro documentado acima |
| **4.** OpenAPI / Swagger | `/swagger-ui.html` |
| **5.** Extrato filtrando por período, cedente e moeda | `GET /api/reports/settlements` |
| **5.** *Diferencial:* SQL nativo em vez de ORM puro | `SettlementReportReader`, com `EXPLAIN` confirmando o uso de índice |
| **6.** Três camadas | `api → domain → infrastructure` |
| **6.** Relatórios em duas camadas | Controller do extrato fala direto com o reader |
| **Front 1.** Input do recebível | Painel do operador |
| **Front 1.** Cálculo líquido em tempo real | Simulação com debounce a cada alteração |
| **Front 2.** Paginação server-side | `Pagination` + `PageResult` do backend |
| **Front 2.** Filtros dinâmicos | Filtros na query string |
| **Front 3.** Separação UI / lógica de estado | Hooks concentram estado; componentes só renderizam props |
| **Front 3.** Estado global *(se necessário)* | TanStack Query para estado de servidor; sem Redux, justificado acima |
| **NF 1.** Tratamento de exceções | `ApiErrorHandler` global, RFC 7807 |
| **NF 2.** Critérios de aceite | Seção acima, em quatro eixos |
| **Pleno.** Conventional Commits | 50 commits |
| **Pleno.** Pull Requests descritivos | 17 PRs |
| **Pleno.** Histórico limpo | Merges por rebase, zero commits de merge |
| **Pleno.** Docker e Docker Compose | Três serviços orquestrados |
| **Pleno.** Exception handler global | `ApiErrorHandler` |
| **Pleno.** Validações de input robustas | Bean Validation em toda entrada + `fail-on-unknown-properties` |
| **Pleno.** Testes unitários das regras de precificação | 14 testes só do motor, dentro dos 63 |
| **7.** Diagrama ER | [`docs/modelo-de-dados.md`](docs/modelo-de-dados.md) |
| **7.** Scripts DDL | [`docs/schema.sql`](docs/schema.sql) + migrações Flyway |
| **2 (política de IA).** `AI_USAGE.md` | [`AI_USAGE.md`](AI_USAGE.md) |

## Limites conhecidos

Coisas que ficaram de fora e o motivo:

- **Sem testes de integração com banco.** Os 63 testes são unitários e de slice. Seis defeitos deste
  projeto atravessaram a suíte verde — entre eles a `LazyInitializationException` na serialização e o
  bean que só quebrava na imagem JRE — e apareceram exercitando a aplicação de verdade. Estão listados
  no [`AI_USAGE.md`](AI_USAGE.md). Testcontainers com `@DataJpaTest` é o próximo passo natural.
- **Sem cadastro de cedente pela interface.** O enunciado cita o cedente como dimensão de filtro do
  extrato, não como entidade a cadastrar — e é isso que está entregue: `GET /api/assignors` alimenta o
  seletor do painel e o filtro do extrato, com dois cedentes no seed. Um `POST` seria trivial, mas
  puxaria junto o que um cadastro de verdade exige: validação de CNPJ com dígito verificador, consulta
  de situação cadastral, análise de crédito do cedente. Preferi não entregar meia porta.
- **Sem autenticação.** Não estava no escopo; num sistema real, cada liquidação precisaria de
  identidade e trilha de quem a executou.
- **Provedor de câmbio é simulado.** A interface existe e a implementação é substituível por um cliente
  HTTP sem tocar no domínio, mas não há retry nem circuit breaker — isso é escopo de sênior.
- **Aspectos fiscais e regulatórios de uma cessão real** (tributos sobre a operação, registro do
  recebível em registradora, coobrigação do cedente) não estão modelados. São exigências reais do
  mercado brasileiro que exigem conhecimento de domínio que ainda não tenho; preferi não fingir que
  tenho a modelar errado.

O uso de IA neste projeto está documentado em [`AI_USAGE.md`](AI_USAGE.md).
