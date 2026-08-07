# Uso de IA neste projeto

Usei IA bastante, em duas frentes. Ela escreveu a maior parte do código, e me ensinou o negócio. A
segunda parte foi a que deu trabalho.

Sou fullstack e não venho do mercado financeiro. Antes deste desafio eu não sabia o que era um FIDC,
nem o que significava deságio, nem por que alguém compraria uma duplicata por menos do que ela vale.
Escrever código foi o que a IA mais acelerou. O difícil foi chegar num ponto em que eu olhasse um
número na tela e soubesse dizer se ele fazia sentido.

Se for para apontar onde foi o tempo desta entrega, foi aí: a maior parte não foi escrevendo código,
foi aprendendo as regras de negócio. Entender por que o prazo é expoente, por que o spread muda com o
tipo do título, por que a conversão entra no fim e não no começo. Enquanto isso não fechava, eu não
tinha como revisar nada do que a IA produzia, só aceitar.

## O que eu não sabia

Comecei pedindo explicação, não código: como o dinheiro circula numa cessão de crédito, quem paga o
quê e quando; por que o fundo compra uma duplicata de R$ 10.000 por R$ 9.500; qual a diferença de
risco entre duplicata e cheque pré-datado.

O que ficou disso, e que sustenta o modelo inteiro:

O fundo está comprando tempo e assumindo risco. A empresa tem um recebível que só vira dinheiro no
vencimento, o fundo antecipa isso hoje e cobra pelo serviço. O deságio é o preço das duas coisas: o
custo do dinheiro no tempo mais o risco de calote.

Daí o spread variar por tipo. A duplicata tem uma nota fiscal por trás, ou seja, mercadoria ou serviço
entregue. O cheque pré-datado é só uma promessa. Mais risco, spread maior, que é o que o enunciado
modela com 1,5% e 2,5% ao mês.

E daí o prazo entrar como expoente. Isso me custou tempo. Se fosse juros simples, dois meses seriam o
dobro de um mês. Como o dinheiro rende sobre o que já rendeu, a fórmula é composta.

## O que eu conferi em vez de aceitar

Minha regra foi recalcular à mão toda regra financeira antes de virar código. Não por desconfiar da
IA, mas porque eu não tinha a intuição que denuncia um número errado.

O caso que definiu isso: pedi um teste para prazo fracionário e o valor esperado veio como R$ 9.632,29
(10.000 descontados a 2,5% ao mês por 45 dias). Fiz a conta separado:

```
45 dias = 1,5 mês comercial
1,025 ^ 1,5 = 1,0377334
10.000 / 1,0377334 = 9.636,39
```

O certo era R$ 9.636,39. Quatro reais em dez mil: pequeno o bastante para passar batido, grande o
bastante para ser defeito. Corrigi antes de rodar, e depois o `big-math` confirmou a minha conta, não
a da IA. Ficou a regra de calcular todo valor esperado por fora, porque um teste cujo valor esperado
veio da mesma fonte que o código não testa nada, só congela o erro.

## Decisões que eu tomei

Em vários pontos a IA listou as opções, mas a escolha tinha consequência em dinheiro e eu precisei
entender antes de decidir.

**De quando conta o prazo.** A primeira sugestão contava da emissão. Pensei no fluxo de caixa: o fundo
espera de hoje até o vencimento, não da emissão. Título emitido há três meses e vencendo amanhã não
vale desconto de três meses. Conta da data de referência.

**HALF_UP ou HALF_EVEN.** O arredondamento que eu conhecia era HALF_UP. Perguntei por que a IA sugeriu
HALF_EVEN e a resposta fez sentido quando pensei em volume: arredondar todo empate para cima cria
viés de alta a favor de um dos lados, e em milhares de operações isso vira dinheiro. Fiquei com
HALF_EVEN e testei o único ponto onde os dois divergem, a meia unidade exata.

**Converter antes ou depois do deságio.** O enunciado manda converter no final, mas eu queria saber
por quê. É porque o valor presente na moeda do título é uma cifra reportável sozinha, é ela que vai
para o extrato. Converter antes dá um líquido que não fecha com o valor presente exibido.

**Total do lote.** Soma dos títulos arredondados, não arredondamento da soma. Os dois dão números
diferentes, e só o primeiro faz o cabeçalho do extrato fechar com o linha a linha, que é o que uma
auditoria confere.

**Travar a cotação.** Essa levantei sozinho: se a operação guardasse só uma referência para a linha de
cotação e aquela linha mudasse, o extrato passaria a mostrar um câmbio diferente do aplicado. A
liquidação guarda o `id` para auditoria e o valor para reprodutibilidade.

**Não inverter par de moedas.** A IA sugeriu usar `1/(BRL/USD)` quando faltasse USD/BRL. Recusei. Cada
ponta tem spread próprio, e taxa calculada por inversão é taxa que ninguém registrou. Melhor recusar a
operação com erro claro do que inventar um câmbio.

## Onde a IA errou

**Número plausível e errado.** O R$ 9.632,29 acima. É o pior tipo de erro num sistema financeiro,
porque não quebra nada, só dá o resultado errado.

**Solução certa no lugar errado.** Para padronizar o corpo de erro da API, a sugestão foi sobrescrever
`createProblemDetail`. Passou nos testes. Mas as exceções que o próprio Spring resolve (rota
inexistente, método errado) não passam por ali, já vêm com o corpo pronto. Dava um `404` fora do
padrão vazando a frase interna `"No static resource ..."`. Só descobri chamando a API com `curl`.
Movi para `createResponseEntity`, por onde tudo passa.

**Funcionava na minha máquina e quebrava no container.** O provedor simulado de cotações usava
`RandomGenerator.getDefault()`. Roda bem em desenvolvimento, com JDK completo, e derruba a aplicação
na imagem JRE do Docker, que não traz o módulo `jdk.random`. Passou por 63 testes sem reclamar.
Apareceu quando subi a stack pelo `docker compose`. Troquei por `ThreadLocalRandom`.

**Substituição em massa que quebrou uma rota.** Na hora de acentuar os textos em português, a
substituição pegou também o caminho da rota do frontend: `path="transacoes"` virou `path="transações"`.
Como o link do menu foi acentuado junto, navegar pela interface continuou funcionando, mas qualquer URL
digitada caía no redirect. Só apareceu abrindo o endereço direto no navegador.

**Seguir a ferramenta teria piorado.** O `npm audit` apontou vulnerabilidade alta no `react-router` e
ofereceu `npm audit fix --force`. Fui verificar antes: o "fix" descia a versão para uma faixa com
catorze advisories em vez de uma. Não existe versão limpa dessa lib hoje. Fiquei na mais corrigida e
documentei. Aceitar a sugestão automática teria trocado uma advisory inaplicável por catorze reais.

## Análise crítica

Onde economizou tempo de verdade: aprender o domínio (semanas viraram horas, e sem isso eu teria só
implementado a fórmula do enunciado sem entender o que ela significa), o vocabulário do setor (cedente,
deságio, valor de face, lastro, liquidação, spread, saber nomear certo foi o que permitiu modelar
tabelas e classes que um analista reconheceria), e volume de código. Aqui vale ser direto: entidades,
DTOs, mapeamentos, componentes React, CSS, `pom.xml`, Dockerfiles, esqueleto de testes e rascunho de
documentação saíram quase todos da IA. É trabalho necessário, extenso e sem decisão embutida, e é o
que explica o volume entregue no prazo. O tempo não veio de digitar rápido, veio de não digitar.

Onde atrapalhou:

A IA explica com a mesma segurança o que está certo e o que está errado. Em código dá para rodar e ver
falhar. Numa regra de negócio que eu não domino, não dá para perceber a diferença sem sair da conversa
e conferir por fora.

Otimismo sobre o ambiente. Seis defeitos deste projeto atravessaram a suíte verde e só apareceram com
a aplicação no ar: o contrato de erro que não cobria as exceções do Spring, o proxy do Hibernate
morrendo fora da transação, a ordenação do Postgres jogando operações pendentes para o topo do extrato,
o `RandomGenerator` ausente na imagem JRE, a rota quebrada pela acentuação e a paginação que se perdia
num link montado à mão. Nenhum era detectável sem subir a aplicação e usar.

Tendência a expandir escopo. Várias sugestões vinham com coisas de nível sênior que o enunciado não
pedia. Cortei para manter a entrega no nível a que ela se propõe.

Três regras que levo adiante: cálculo financeiro se confere por fora, à mão, antes de virar teste;
código só está pronto depois de rodar de verdade (toda funcionalidade daqui foi exercitada com `curl`
contra o Postgres real e no navegador contra a API real, foi assim que os seis defeitos apareceram); e
sugestão de ferramenta também se verifica, inclusive `npm audit fix`.

## O que eu domino e o que não domino

O que é meu neste repositório são as decisões e a verificação. A IA escreveu a maior parte das linhas,
eu decidi a forma e conferi o resultado. Consigo explicar cada escolha e o que ela custa: por que o
lote é um agregado, por que o lock é otimista, por que o extrato não passa pelo ORM, por que o `422` é
diferente do `400`, por que o total é a soma dos arredondados.

Meu perfil é fullstack. Conheço Java e Spring o suficiente para decidir a arquitetura e revisar o que a
IA produz, mas não me apresento como especialista em Java. O que eu trago é tocar o sistema inteiro, do
schema à tela, e enxergar a consequência de uma decisão nas duas pontas.

Esta entrega saiu em poucas horas porque essas horas não foram gastas digitando. Foram gastas
aprendendo a operação, decidindo a modelagem e conferindo o resultado, nessa ordem de custo. Aprender
o negócio levou mais tempo que escrever o sistema, e a conferência levou mais tempo que a implementação.
É o que justifica a entrega: aceitando o que a IA produziu sem verificar, eu teria terminado bem antes
e entregue com seis defeitos dentro.

Não domino o mercado de crédito. Entendo a mecânica de uma operação de desconto de recebíveis e sei
justificar a modelagem, mas uma operação real tem camadas que não estão aqui e que eu não saberia
modelar direito: tributação, registro do recebível em registradora, coobrigação do cedente,
inadimplência e recompra. Deixei tudo isso fora de escopo no README em vez de implementar uma versão
errada.

Se isso fosse para produção, o primeiro passo que eu pediria não seria técnico: seria sentar com
alguém da mesa de operações e conferir se o que modelei é o que eles fazem.
