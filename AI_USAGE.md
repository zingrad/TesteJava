# Uso de IA neste projeto

Usei IA de forma intensa aqui, em duas frentes bem diferentes: ela **escreveu a maior parte do código**
e me **ensinou o negócio**. A segunda foi a que exigiu mais de mim.

Sou desenvolvedor fullstack e **não venho do mercado financeiro**. Antes deste desafio eu não sabia o
que era um FIDC, o que significa "deságio", nem por que alguém compraria uma duplicata por menos do que
ela vale. Escrever o código foi a parte que a IA mais acelerou. A parte difícil foi chegar num ponto em
que eu conseguisse olhar para um número na tela e dizer se ele fazia sentido.

Este documento é honesto sobre isso, porque um sistema financeiro construído por quem não entende a
operação é um risco — e eu preferia declarar o risco a escondê-lo.

---

## 1. O que eu não sabia, e como fechei a lacuna

### O que eu precisava entender antes de escrever qualquer linha

Comecei pedindo à IA que me explicasse o problema de negócio como se eu fosse alguém de fora, não que
me desse código. Os prompts que mais renderam foram nessa linha:

> "Explique como funciona uma operação de cessão de crédito num FIDC do ponto de vista do fluxo de
> dinheiro: quem paga o quê, para quem, e quando. Não me dê código."

> "Por que um fundo compra uma duplicata de R$ 10.000 por R$ 9.500? O que ele está sendo remunerado
> por, exatamente?"

> "Qual a diferença de risco entre uma duplicata mercantil e um cheque pré-datado, e por que isso
> justifica spreads diferentes?"

O que consolidei disso, e que sustenta o modelo inteiro:

- **O fundo está comprando tempo e assumindo risco.** A empresa cedente tem um recebível que só vira
  dinheiro no vencimento; o fundo antecipa esse dinheiro hoje e cobra por isso. O deságio é o preço
  dessas duas coisas juntas — o custo do dinheiro no tempo (taxa base) mais o risco de o título não
  ser pago (spread).
- **Por isso o spread varia por tipo.** A duplicata é lastreada numa nota fiscal, ou seja, existe uma
  mercadoria ou serviço entregue por trás dela. O cheque pré-datado é só uma promessa. Mais risco,
  spread maior — e é exatamente isso que o enunciado modela com 1,5% e 2,5% ao mês.
- **Por isso o prazo entra como expoente, e não como multiplicador.** Isso me custou tempo para
  entender. Se fosse juros simples, o desconto de dois meses seria o dobro do de um mês. Como o
  dinheiro rende sobre o que já rendeu, a fórmula é composta, e o prazo é potência.

### O que eu conferi em vez de aceitar

Meu critério foi: **toda regra de negócio que a IA me explicou, eu recalculei à mão antes de virar
código.** Não porque desconfio da IA em geral, mas porque no meu caso eu não teria como perceber um
erro só olhando — eu não tinha a intuição que denuncia um número errado.

O caso mais útil foi este. Pedi um teste para o prazo fracionário e o valor esperado veio como
**R$ 9.632,29** para 10.000 descontados a 2,5% ao mês por 45 dias. Fui fazer a conta separado:

```
45 dias = 1,5 mês comercial
1,025 ^ 1,5 = 1,0377334
10.000 / 1,0377334 = 9.636,39
```

O número certo era **R$ 9.636,39**. A diferença é de quatro reais em dez mil — pequena o bastante para
passar despercebida, grande o bastante para ser um defeito real. Corrigi antes de rodar, e depois o
`big-math` confirmou o meu cálculo, não o da IA.

Foi o momento em que decidi que **todo valor esperado nos testes de precificação seria calculado por
mim antes de rodar a suíte**. Um teste cujo valor esperado veio da mesma fonte que o código não testa
nada: só congela o erro.

### Decisões de negócio que a IA não decidiu por mim

Em vários pontos a IA apresentou as opções, mas a escolha tinha consequência financeira e eu precisei
entender antes de decidir:

**De quando conta o prazo?** A primeira sugestão contava da emissão do título. Parei e pensei no fluxo
de dinheiro: o fundo vai esperar do *hoje* até o vencimento, não da emissão. Um título emitido há três
meses e vencendo amanhã não vale um desconto de três meses. O prazo conta da data de referência.

**`HALF_UP` ou `HALF_EVEN`?** O arredondamento comercial que eu conhecia é o `HALF_UP`. Perguntei por
que a IA sugeriu `HALF_EVEN` e a resposta fez sentido quando pensei em volume: arredondar todo empate
para cima cria um viés de alta sistemático a favor de um dos lados. Em milhares de operações isso
deixa de ser arredondamento e vira dinheiro. Fiquei com `HALF_EVEN` e escrevi um teste no único ponto
onde os dois divergem — a meia unidade exata.

**Converter antes ou depois do deságio?** O enunciado diz "aplicar a conversão cambial no final", mas
eu queria entender *por quê*. É porque o valor presente na moeda do título é uma cifra reportável por
si só: é ela que vai para o extrato e para o banco. Converter antes produziria um líquido que não
fecha com o valor presente exibido.

**O total do lote é a soma dos títulos arredondados ou o arredondamento da soma?** Os dois dão números
ligeiramente diferentes. Escolhi a soma dos arredondados porque é a única forma de o cabeçalho do
extrato fechar com o linha a linha — que é o que uma auditoria confere.

**Travar a cotação.** Essa levantei por conta própria depois de entender o resto: se a operação só
guardasse uma referência para a linha de cotação, e aquela linha mudasse, o extrato passaria a
reproduzir um câmbio diferente do que foi aplicado. A liquidação guarda o `id`, para auditoria, **e**
o valor, para reprodutibilidade.

**Não inverter par de moedas implicitamente.** A IA sugeriu usar `1/(BRL/USD)` quando faltasse a
cotação USD/BRL. Recusei: na prática cada ponta tem spread próprio, e uma taxa calculada por inversão
é uma taxa que ninguém registrou. Prefiro recusar a operação com erro claro a inventar um câmbio.

---

## 2. Onde a IA errou

Cinco casos concretos deste projeto:

**Um número plausível e errado.** Já descrito: R$ 9.632,29 em vez de R$ 9.636,39. É o tipo de erro mais
perigoso num sistema financeiro, porque não quebra nada — só dá o resultado errado.

**Uma solução correta no lugar errado.** Para padronizar o corpo de erro da API, a sugestão foi
sobrescrever `createProblemDetail`. Parecia certo e passou nos testes. Mas as exceções que o próprio
Spring resolve — rota inexistente, método errado — não passam por esse método; elas já trazem o corpo
pronto. O resultado era um `404` fora do padrão, vazando a frase interna `"No static resource ..."`.
Só descobri chamando a API subida com `curl`. Movi o tratamento para `createResponseEntity`, que é o
único ponto por onde todas as respostas passam.

**Código que funcionava na minha máquina e quebrava no container.** O provedor simulado de cotações
usava `RandomGenerator.getDefault()`. Roda perfeitamente em desenvolvimento, onde há JDK completo, e
derruba a aplicação na imagem JRE do Docker, que não traz o módulo `jdk.random`. Passou por 63 testes
sem reclamar. Só apareceu quando subi a stack inteira pelo `docker compose`. Troquei por
`ThreadLocalRandom`.

**Uma substituição automática que quebrou uma rota.** Na hora de acentuar os textos em português,
apliquei uma substituição em massa. Ela acentuou também o caminho da rota do frontend:
`path="transacoes"` virou `path="transações"`. Como o link do menu foi acentuado junto, a navegação
pela interface continuou funcionando — mas qualquer URL digitada ou compartilhada caía no redirect. Só
apareceu abrindo o endereço direto no navegador.

**Um caso em que seguir a ferramenta teria piorado tudo.** O `npm audit` apontou uma vulnerabilidade
alta no `react-router` e ofereceu `npm audit fix --force`. Fui verificar antes de rodar: o "fix"
descia a versão para uma faixa com **catorze** advisories em vez de uma. Não existe versão limpa dessa
biblioteca hoje. Fiquei na mais corrigida e documentei o porquê. Se eu tivesse aceitado a sugestão
automática, teria trocado uma advisory inaplicável por catorze reais.

---

## 3. Análise crítica

### Onde economizou tempo de verdade

- **Aprendizado de domínio.** Semanas viraram horas. Sem isso eu não teria entregue este desafio com
  esta modelagem — no máximo teria implementado a fórmula do enunciado sem entender o que ela
  significa.
- **Vocabulário técnico do setor.** Cedente, deságio, valor de face, valor presente, lastro,
  liquidação, spread. Saber nomear as coisas certo foi o que permitiu modelar tabelas e classes com
  nomes que um analista do mercado reconheceria.
- **Volume de código.** Aqui é onde vale ser direto: entidades, DTOs, mapeamentos, componentes React,
  CSS, `pom.xml`, Dockerfiles, o esqueleto dos testes e o rascunho da documentação saíram quase todos
  da IA. É trabalho necessário, extenso e sem decisão embutida — e é o que explica o volume entregue
  no prazo. O tempo não veio de digitar mais rápido; veio de não digitar.
- **Levantar alternativas.** Em quase toda decisão de arquitetura, a IA foi boa em listar as opções e
  os trade-offs. A escolha ficou comigo, mas partir de três opções descritas é melhor que partir do
  zero.

### Onde atrapalhou

- **Confiança uniforme.** A IA explica com a mesma segurança o que está certo e o que está errado. No
  código dá para rodar e ver falhar; numa regra de negócio que eu não domino, não há como perceber a
  diferença sem sair da conversa e conferir por fora.
- **Testes que só confirmam o código.** Se o valor esperado do teste vem da mesma fonte que a
  implementação, o teste não prova nada. Precisei me impor a regra de calcular os valores
  esperados por fora.
- **Otimismo sobre o ambiente.** Seis defeitos deste projeto atravessaram a suíte de testes verde e só
  apareceram com a aplicação no ar: o contrato de erro que não cobria as exceções resolvidas pelo
  próprio Spring, o proxy do Hibernate morrendo fora da transação, a ordenação do Postgres jogando as
  operações pendentes para o topo do extrato, o `RandomGenerator` ausente na imagem JRE, a rota que a
  acentuação automática quebrou e a paginação que se perdia num link montado à mão. Nenhum deles era
  detectável sem subir a aplicação e usá-la.
- **Tendência a expandir escopo.** Várias vezes a sugestão veio com coisas de nível sênior que o
  enunciado não pedia. Cortei para manter a entrega no nível a que ela se propõe.

### O que ficou como método

Três regras que adotei durante o projeto e que levo adiante:

1. **Cálculo financeiro se confere por fora.** À mão, em fonte separada, antes de virar teste.
2. **Código só está pronto depois de rodar de verdade.** Suíte verde não é evidência suficiente:
   toda funcionalidade deste projeto foi exercitada com `curl` contra o Postgres real e no navegador
   contra a API real. Foi assim que os seis defeitos acima apareceram.
3. **Sugestão de ferramenta também se verifica.** Inclusive `npm audit fix`.

---

## 4. O que eu domino e o que não domino

**O que é meu neste repositório são as decisões e a verificação.** A IA escreveu a maior parte das
linhas; eu decidi a forma e conferi o resultado. Consigo explicar cada escolha e o que ela custa: por
que o lote é um agregado, por que o lock é otimista e não pessimista, por que o extrato não passa pelo
ORM, por que o `422` é diferente do `400`, por que o total é a soma dos arredondados. Nenhuma dessas
entrou sem eu entender a consequência.

**Meu perfil é fullstack.** Domino os conceitos de Java e do ecossistema Spring o suficiente para
decidir a arquitetura e revisar o que a IA produz, mas não me apresento como especialista em Java. O
que eu trago é conseguir tocar o sistema inteiro — do schema à tela — e enxergar a consequência de uma
decisão nas duas pontas.

**Sobre o tempo.** Esta entrega saiu em poucas horas, e isso só é possível porque essas horas não foram
gastas digitando. Foram gastas decidindo a modelagem e conferindo o que saía. A conferência é que
consumiu o tempo, e é ela que justifica a entrega: se eu tivesse aceitado o que a IA produziu sem
verificar, teria terminado antes e entregue com seis defeitos dentro.

**Não domino o mercado de crédito.** Entendo hoje a mecânica de uma operação de desconto de recebíveis
e sei justificar a modelagem. Mas uma operação real tem camadas que não estão aqui e que eu não teria
condição de modelar corretamente: tributação da operação, registro do recebível em registradora,
coobrigação do cedente, tratamento de inadimplência e recompra. Deixei tudo isso explicitamente fora
de escopo no README em vez de implementar uma versão errada.

Se este código fosse para produção, o passo que eu pediria antes de qualquer outro não seria técnico:
seria sentar com alguém da mesa de operações e conferir se o que eu modelei é o que eles fazem.
