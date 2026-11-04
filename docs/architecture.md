# Arquitetura

O projeto é um **monolito em camadas**, com **DDD leve** e **CQRS leve**. Este documento explica o que isso significa
na prática, como as pastas estão organizadas e quais regras valem para o código. Ele é atualizado conforme o curso
avança: quando uma aula mudar a estrutura, este arquivo muda junto.

---

## 1. Monolito em camadas

### Monolito
É uma aplicação só: um projeto, um `.jar`, um banco de dados. Os módulos (cozinhas, restaurantes, pagamentos) vivem
no mesmo processo e conversam por chamadas de método comuns, sem rede entre eles.

### Camadas
Cada módulo é dividido em quatro camadas, e cada uma tem uma responsabilidade:

| Camada | Responsabilidade | Exemplos |
|---|---|---|
| `presentation` | Falar com o mundo externo (HTTP) | Controllers, DTOs de entrada e saída |
| `application` | Coordenar cada caso de uso | Services, comandos, views, exceções de aplicação |
| `domain` | Guardar as regras do negócio | Agregados, value objects, interfaces de repositório |
| `infrastructure` | Resolver os detalhes técnicos | JPA, banco de dados, integrações, configurações |

A regra que organiza tudo é a **direção das dependências**:

```
presentation  →  application  →  domain  ←  infrastructure
```

O controller chama o service, o service usa o domínio, e a infraestrutura **implementa** o que o domínio precisa.
O domínio não conhece ninguém: não importa Spring, JPA nem nada de web. As regras de negócio podem ser
testadas com um teste simples de JUnit, sem subir o contexto do Spring.

Regras de cada camada:

- **`domain`**: Java puro. Sem anotações de Spring, JPA ou JSON.
- **`application`**: só conhece o `domain`. Pode usar `@Service` e `@Transactional`, mas não importa JPA, classes de
  web nem nada de `infrastructure` ou `presentation`. Quando precisa de algo externo, define uma interface (um *port*).
- **`infrastructure`**: implementa os ports. É a única camada que conhece JPA.
- **`presentation`**: só chama a `application`. Nunca usa repositórios, agregados ou adapters diretamente. A única
  exceção é o `GlobalExceptionHandler`, que enxerga o `DomainException` para traduzi-lo em HTTP 422. Os enums do
  domínio chegam à `presentation` como `String`, e quem converte é a `application` (seção 10).

### Camadas e Clean Architecture

Mistura as duas coisas. A estrutura é de **camadas em pacotes, dentro de módulos** (um monolito
modular), mas a **direção das dependências** segue a ideia da Clean Architecture e da arquitetura hexagonal:

- Numa arquitetura em camadas tradicional (usada no curso), o negócio chama a camada de dados e **depende** dela.
- Aqui é invertido: o domínio define a interface (`KitchenRepository`) e a infraestrutura **depende do domínio** para
  implementá-la. O domínio fica no centro e não conhece o banco.

Diferenças em relação à Clean Architecture "de livro": não há um círculo para cada coisa nem uma classe por caso de
uso (os casos de uso ficam agrupados em services), e a `application` aceita `@Service` e `@Transactional` por
pragmatismo. O DDD é um assunto à parte e combina com as duas: é ele que define como o modelo de domínio é escrito
(agregados, value objects, regras dentro das classes).

---

## 2. Organização das pastas

A divisão é feita primeiro por **módulo** (a área do negócio) e depois por camada. `<pacote.raiz>` é o pacote
principal do projeto:

```
src/main/java/<pacote.raiz>/
│
├── shared/                          # o que é comum a todos os módulos
│   ├── domain/                      #   DomainException, AggregateRoot (opcional)
│   ├── application/                 #   NotFoundException, ConflictException
│   ├── presentation/                #   GlobalExceptionHandler
│   └── infrastructure/config/       #   configurações gerais (ex.: segurança, quando entrar)
│
├── kitchen/                         # um módulo
│   ├── api/                         #   o que OUTROS módulos podem usar (criado quando algum precisar)
│   │
│   ├── presentation/                #   camada web
│   │   ├── KitchenController
│   │   ├── KitchenWebMapper         #     DTO <-> comando/view
│   │   └── dto/                     #     CreateKitchenRequest, KitchenResponse
│   │
│   ├── application/                 #   casos de uso
│   │   ├── KitchenCommandService    #     escrita (cria, altera, remove)
│   │   ├── KitchenQueryService      #     leitura
│   │   ├── command/                 #     CreateKitchenCommand
│   │   ├── view/                    #     KitchenView (objeto de leitura)
│   │   ├── port/                    #     KitchenQueryRepository (interface de leitura)
│   │   └── exception/               #     KitchenNotFoundException, KitchenNameAlreadyExistsException
│   │
│   ├── domain/                      #   regras do negócio
│   │   ├── Kitchen                  #     o agregado
│   │   ├── KitchenRepository        #     interface de escrita (port)
│   │   └── vo/                      #     KitchenId, KitchenName
│   │
│   └── infrastructure/
│       └── persistence/             #   tudo de JPA fica aqui
│           ├── KitchenEntity        #     espelho da tabela
│           ├── KitchenMapper        #     Kitchen <-> KitchenEntity
│           ├── SpringDataKitchenRepository         # (escrita, só do pacote)
│           ├── JpaKitchenRepositoryAdapter         # implementa KitchenRepository
│           ├── SpringDataKitchenQueryRepository    # (leitura, só do pacote)
│           └── JpaKitchenQueryRepositoryAdapter    # implementa KitchenQueryRepository
│
├── restaurant/                      # outro módulo, com a mesma estrutura
│   └── infrastructure/acl/          #   ponte para o módulo kitchen (veja a seção 5)
│
└── payment/                         # outro módulo, com a mesma estrutura
```

Algumas observações:

- Os pacotes só são criados quando têm algo dentro. Não é preciso montar toda a árvore de uma vez.
- Os repositórios do Spring Data ficam **sem `public`** (visíveis só dentro do pacote `persistence`). Quem usa o
  repositório de fora enxerga apenas as interfaces do domínio e da aplicação.
- Os testes seguem a mesma estrutura de pacotes em `src/test/java`.

---

## 3. DDD leve

Aqui são usadas só as partes que trazem benefício para um projeto deste tamanho.

### O que é usado
- **Domínio rico.** A regra de negócio fica dentro da própria classe, não nos services. Um `Restaurant` sabe trocar de
  nome e de cozinha, e recusa o que for inválido. O service apenas chama `restaurant.changeName(newName)` e o
  restaurante decide se é permitido.
- **Value Objects.** Em vez de `String name`, existe a classe `KitchenName`, que valida e normaliza no construtor. Se há
  um `KitchenName` na mão, ele é válido, e não precisa ser conferido de novo. São `record`s imutáveis.
- **Aggregate Root.** É a "porta de entrada" de um grupo de dados. Hoje os agregados (`Kitchen`, `Restaurant`,
  `Payment`) têm só a raiz. Se um deles ganhar partes, elas só são alteradas passando pela raiz.
- **Repositório como interface do domínio.** O domínio define `KitchenRepository` (`save`, `findById`), e a
  infraestrutura implementa com Spring Data. O domínio pensa em "coleção de cozinhas", sem saber de JPA.
- **Fábricas com nome.** `Kitchen.create(...)` cria uma cozinha nova, validando tudo. `Kitchen.restore(...)`
  reconstrói uma que veio do banco, cujos dados já foram validados quando foram salvos.

### O que fica de fora (por enquanto)
Eventos de domínio, contextos delimitados (*bounded contexts*) formais e serviços de domínio elaborados.

---

## 4. Dois modelos: domínio e persistência

Cada agregado tem **duas representações**, ligadas por um mapper:

- a classe de **domínio** (`Kitchen`, `Restaurant`): tem as regras, não conhece o banco;
- a classe de **persistência** (`KitchenEntity`, `RestaurantEntity`): é o espelho da tabela, cheia de anotações JPA e
  sem regra de negócio.

Isso dá mais trabalho, mas o banco deixa de ditar como o domínio é escrito (e vice-versa).

### Como os objetos se referenciam

Aqui existem **duas regras diferentes**, uma para cada modelo:

| | Modelo de domínio | Modelo de persistência |
|---|---|---|
| Outro **agregado** | só pelo **id** (`KitchenId`) | pode ser **direto** (`@ManyToOne KitchenEntity`) |
| Dentro do **mesmo agregado** | objeto direto (o agregado contém as suas partes) | relação normal (`@OneToMany`, `@Embedded`) |

O projeto ainda não tem um agregado com partes, então a segunda linha vale para quando houver. Um exemplo da
primeira:

```java
// DOMÍNIO: o restaurante conhece a cozinha só pelo id
class Restaurant {
    private KitchenId kitchenId;
    private RestaurantName name;
}

// PERSISTÊNCIA: o restaurante aponta para a entidade da cozinha, como no banco (chave estrangeira)
@Entity
class RestaurantEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    private KitchenEntity kitchen;

    private String name;
}
```

**Por que no domínio é só pelo id:** evita carregar um agregado dentro do outro, mantém cada um independente e deixa
claro onde fica a fronteira de consistência. Quem precisa dos dados da cozinha pede ao módulo `kitchen`.

**Por que na persistência pode ser direto:** ali o modelo descreve o banco, e no banco a ligação é uma chave
estrangeira. Usar a associação do JPA traz a FK criada pelo próprio mapeamento e permite consultas que naveguem entre
as tabelas.

### Como o mapper faz a ponte
Ao salvar, o mapper recebe o `KitchenId` do domínio e precisa entregar uma `KitchenEntity` ao JPA. Ele não acessa o
banco. Quem chama (o adapter, dentro de uma transação) cria uma **referência** com
`entityManager.getReference(KitchenEntity.class, id)`, que não faz consulta: devolve um objeto que só conhece o id, o
suficiente para gravar a coluna. Ao ler, o caminho inverso é `entity.getKitchen().getId()`, e com `LAZY` isso também
não consulta nada.

### Cuidados
- **Sempre `LAZY`** nas associações `@ManyToOne`. O padrão do JPA é `EAGER`, que carregaria a cozinha junto com
  cada restaurante, mesmo sem ninguém precisar.
- **O mapper lê só o id da cozinha.** Com `LAZY`, `getKitchen().getId()` funciona porque o proxy já guarda o id. Ler
  outro campo (`getKitchen().getName()`) depois que a sessão fechou lança `LazyInitializationException`. Se o domínio
  precisar de mais dados da cozinha, ele pede à `kitchen.api`, não à entidade.
- **Partes do mesmo agregado** (`@OneToMany`) são carregadas junto com a raiz, com `join fetch` ou `@EntityGraph`, na
  consulta que o adapter usa. O mapper converte para o domínio depois que a sessão do JPA fechou, e uma coleção
  `LAZY` ainda não carregada lançaria `LazyInitializationException` nesse momento.
- Usar a entidade de outro módulo é um acoplamento aceito, mas fica **restrito ao pacote
  `infrastructure.persistence`** (regra do projeto, seção 10). Domínio, aplicação e web enxergam apenas ids.

---

## 5. Conversa entre módulos

Um módulo não mexe nas coisas internas de outro. Para o comportamento (as regras e as perguntas), a comunicação
passa por duas peças:

- **`api`** (no módulo que oferece): um pacote com a interface pública, por exemplo `KitchenModuleApi.existsById(id)`.
  É o único pacote que os vizinhos podem importar.
- **`infrastructure/acl`** (no módulo que consome): uma classe que implementa um port do próprio módulo e traduz a
  chamada. É o *Anti-Corruption Layer*, a "alfândega" entre os vocabulários. Exemplo: `restaurant` define o port
  `KitchenLookup`, e `KitchenModuleKitchenLookup` o implementa chamando `kitchen.api`.

Resumo das permissões entre módulos:

| Quem | Pode usar |
|---|---|
| `domain`, `application`, `presentation` de um módulo | nada do outro módulo: usam ids próprios e ports próprios (o ACL implementa o port) |
| `infrastructure.acl` | a `api` do outro módulo |
| `infrastructure.persistence` | as **entidades JPA** do outro módulo (para as associações) |

A linha da persistência é a exceção assumida; o motivo e o custo estão na seção 9.

As dependências entre módulos têm **uma direção só** e não podem formar ciclo: o `restaurant` conhece o `kitchen`, e
o `kitchen` não conhece o `restaurant`.

### Ids próprios
O `restaurant` tem o **seu** `KitchenId` (em `restaurant/domain/vo`), sem reaproveitar o de `kitchen`. Os dois têm o
mesmo nome e ficam em pacotes diferentes, e isso é de propósito: cada módulo fala o seu vocabulário. Atenção ao
import: o IDE pode sugerir o `KitchenId` do outro módulo, então confira o pacote.

### E quando o `kitchen` precisa saber de `restaurant`?
Um caso real: **excluir uma cozinha que ainda tem restaurantes.** O `kitchen` não pode consultar o `restaurant`
(seria um ciclo). A solução é deixar a **chave estrangeira** do banco recusar o `DELETE`: o `GlobalExceptionHandler`
traduz a `DataIntegrityViolationException` em HTTP 409. A FK é a única garantia que não perde a corrida contra um
restaurante criado no mesmo instante, e o custo é a mensagem do 409 ser genérica.

---

## 6. CQRS leve

CQRS é separar **comandos** (quem altera dados) de **consultas** (quem só lê). Na versão pesada isso envolve bancos
separados, filas e eventos. Aqui a versão é bem mais simples, só uma separação de código.

- **Escrita:** `KitchenCommandService` cria ou carrega o **agregado**, aplica as regras do domínio e salva pelo
  `KitchenRepository`.
- **Leitura:** `KitchenQueryService` busca no banco já montando um objeto de leitura (`KitchenView`) com
  `select new ...`. Não monta o agregado nem revalida nada, porque ler não altera nada.

```
POST /kitchens       -> Controller -> KitchenCommandService -> Kitchen (domínio) -> KitchenRepository -> banco
GET  /kitchens/{id}  -> Controller -> KitchenQueryService   -> KitchenQueryRepository -> KitchenView
```

Por que separar:
- a leitura fica mais rápida e simples (sem agregado, sem validar cada linha lida);
- a escrita fica concentrada nas regras;
- uma tela que junta dados de vários lugares vira uma consulta nova, sem deformar o domínio.

O banco é o **mesmo** nos dois lados, sem filas, sincronização ou eventos.

### Cuidados
- O critério de busca da leitura é um tipo simples (`Long`, `String`), não um value object: um termo de busca não é um
  "nome válido" (buscar `a` deve dar "não encontrado", não "nome curto demais").
- **Armadilha do Spring Data:** numa interface `Repository<E, ID>` que devolve `View`, não dê a um método o nome de um
  método do CRUD (`findById`, `findAll`, `existsById`). O Spring Data o roteia para a implementação base, que devolve a
  entidade, e o erro só aparece depois, como `ClassCastException`. Use nomes como `findViewById`. Teste unitário com mock
  não pega isso; só um teste que vai ao banco.

---

## 7. Exemplo de ponta a ponta

`POST /kitchens`:

1. O **controller** recebe o JSON, faz a validação básica (campo vazio, tamanho) e converte o DTO em um comando.
2. O **service** (`application`) cria o value object (`KitchenName`), confere se o nome já está em uso e
   chama `Kitchen.create`.
3. O **domínio** garante que a cozinha nasce válida.
4. O **adapter** (`infrastructure`) converte `Kitchen` em `KitchenEntity` com o mapper e salva com Spring Data.
5. O resultado volta como `KitchenView` e o controller o transforma em `KitchenResponse`.

Cada passo está em uma camada. Se o banco mudar, só o passo 4 muda.

`POST /restaurants` tem os mesmos passos, com uma pergunta a mais no passo 2: o service pergunta ao port
`KitchenLookup` se a cozinha existe. Quem responde é o ACL (`KitchenModuleKitchenLookup`), chamando `kitchen.api`.
Se não existir, o service lança uma exceção que vira 404.

### Como os erros viram HTTP
O `GlobalExceptionHandler` (em `shared/presentation`) devolve tudo como `ProblemDetail`:

| Exceção | Onde nasce | HTTP |
|---|---|---|
| `DomainException` | `domain` (regra de negócio) | 422 |
| `NotFoundException` (`KitchenNotFoundException`...) | `application` | 404 |
| `ConflictException` (`KitchenNameAlreadyExistsException`...) | `application` | 409 |
| `DataIntegrityViolationException` (unique, FK) | banco | 409, com mensagem genérica |
| `MethodArgumentNotValidException` (`@Valid` no Request) | `presentation` | 400, com os erros por campo |
| `HttpMessageNotReadableException` (JSON malformado ou corpo ausente) | `presentation` | 400, com mensagem genérica |

A mensagem do banco (SQL, nome de constraint) e a do Jackson (nomes internos de classe) vão só para o log, nunca para
o cliente.

A validação acontece em **duas camadas**: o Request valida na borda (mensagem amigável por campo) e o value object
guarda a regra de verdade (`DomainException`). Os comandos não validam nada. Um valor de enum desconhecido (a `String`
não corresponde a nenhuma constante) é recusado pela fábrica `from` do próprio enum, com `DomainException`, ou seja,
422 (seção 10).

---

## 8. Padrões usados

| Padrão | Onde | Para quê |
|---|---|---|
| Repository | `KitchenRepository` | O domínio fala em coleção de agregados, sem saber de JPA |
| Adapter (Ports & Adapters) | `JpaKitchenRepositoryAdapter` | Implementa o port do domínio com Spring Data |
| Mapper | `KitchenMapper`, `*WebMapper` | Isola os modelos: domínio ↔ JPA ↔ DTO |
| Service Layer | `*CommandService`, `*QueryService` | Um ponto de orquestração e de transação por caso de uso |
| Facade | `<modulo>.api` | Único contrato que os outros módulos enxergam |
| Anti-Corruption Layer | `<modulo>.infrastructure.acl` | Traduz o vocabulário de um módulo para o de outro |
| Factory Method | `Kitchen.create`, `Kitchen.restore` | Criação válida, com a intenção no nome |
| Value Object | `KitchenName`, `RestaurantName`... | Validação e normalização dentro do tipo |

---

## 9. Custos desta escolha

- **Mais classes** do que num CRUD direto.
- **Mais mapeamentos** (domínio ↔ entidade ↔ DTO). É o preço de manter os modelos separados.
- **Enums viram `String` na `presentation`**: cada enum ganha uma fábrica `from(String)` e a conversão de volta
  (seção 10). É o mesmo preço, aplicado a um valor pequeno.
- **Mais trabalho no começo**, em troca de mudanças mais fáceis depois.


### Trade-off: apontar para a entidade JPA de outro módulo

No domínio, `Restaurant` guarda só um `KitchenId`. Na persistência, `RestaurantEntity` aponta direto para a
`KitchenEntity` (`@ManyToOne`, LAZY). Foi uma escolha consciente, e ela tem custo:

| | `@ManyToOne KitchenEntity` (adotado) | `Long kitchenId` (alternativa estrita) |
|---|---|---|
| Chave estrangeira | Criada pelo próprio mapeamento | Escrita à mão (SQL ou Flyway) |
| Consultas | `r.kitchen.name` navega direto | Join manual ou chamada à `kitchen.api` |
| Mapper | Precisa do `getReference` | Só copia o `Long` |
| Independência do módulo | `restaurant` depende da `KitchenEntity`, que precisa ser `public` | A `KitchenEntity` pode ser `package-private`, e o `kitchen` muda o schema à vontade |
| Extrair o módulo para outro serviço | Reescreve entidade, mapper, adapter e consultas | Quase nada muda |
| Proteção | Exige disciplina: a regra da seção 10, por revisão (um teste de arquitetura poderá travá-la no futuro) | A própria estrutura impede |

O acoplamento é de **schema**, não de domínio: o domínio de `restaurant` só conhece o `KitchenId`. O que fica acoplado
é a persistência dos dois módulos, e só ela, porque a regra do projeto não deixa o resto do `restaurant` enxergar a
`KitchenEntity`.


---

## 10. Regras

- o `domain` não importa Spring, JPA nem JSON;
- o `presentation` não acessa `domain` nem `infrastructure` (a exceção é o `DomainException`, traduzido pelo
  `GlobalExceptionHandler`);
- a `application` não acessa `infrastructure` nem `presentation`, e do Spring só usa `@Service` e `@Transactional`;
- um módulo só usa a `api` do outro, e as entidades JPA do outro só aparecem em `infrastructure.persistence`;
- não há ciclos entre módulos;
- cada tipo de classe está no pacote certo (controllers em `presentation`, entidades JPA em
  `infrastructure.persistence`...).

Por enquanto essas regras valem por convenção e por revisão do código. No futuro, um teste de arquitetura com o
**ArchUnit** poderá conferi-las automaticamente a cada build (item 7.7 do `TODO.md`).

### Enums do domínio

Um enum que descreve o negócio (forma de pagamento, status) mora no `domain` do módulo dono dele, ao lado do
agregado: `payment/domain/PaymentMethod`. Não vai em `vo/`, que é para os records.

A `presentation` **não conhece o enum**: ela só trabalha com `String`. Quem converte é a `application`.

| Camada | Como o enum aparece |
|---|---|
| `presentation` | `String` no Request e no Response |
| `application` | `String` no comando e na view; o service chama `PaymentMethod.from(command.method())` e, na saída, usa `name()` |
| `domain` | o enum e a fábrica `from(String)` |
| `infrastructure` | o enum na entidade JPA, com `@Enumerated(EnumType.STRING)` (nunca `ORDINAL`) |

```java
// domain: a fábrica recusa o valor desconhecido (DomainException -> 422)
public static PaymentMethod from(String value) {
    return switch (value) {
        case "CREDIT_CARD" -> CREDIT_CARD;
        case "PIX"         -> PIX;
        default -> throw new DomainException("Unknown payment method: " + value);
    };
}
```

- **Por que `String` na `presentation`:** a regra "`presentation` não acessa `domain`" fica sem exceção para enums. Um
  enum com comportamento (por exemplo, a transição de status) não fica ao alcance do controller, e a `presentation`
  não precisa saber que ele existe.
- **Por que `switch` explícito e não `valueOf`:** o valor aceito pela API fica num literal, separado do nome da
  constante. Renomear a constante no código não muda o que a API aceita. Na saída, `name()` ainda liga o valor
  devolvido ao nome da constante; se os dois precisarem divergir, a conversão de saída também vira um `switch`
  (por exemplo, `toApiValue()`).
- **O que continua acoplado:** por padrão, o valor da API é o nome da constante, e o banco guarda esse nome
  (`STRING`). Renomear uma constante muda a saída da API e exige migração do banco. O acoplamento deixa de ser de
  código e passa a ser de contrato, explícito na `application`. O teste de API fixa os valores do JSON.
- **O custo:** cada enum ganha a fábrica `from(String)` e a conversão de volta; um valor desconhecido vira 422 (regra
  de domínio), não 400; e o springdoc não lista os valores sozinho (precisa de `@Schema(allowableValues = ...)`).
- **Quando vai para `shared/domain`:** só se outro módulo também precisar dele. O `shared` é o que é comum a todos os
  módulos.
- **Regra:** não há exceção para enums. A `presentation` só pode depender do `DomainException`.

---

## 11. Decisões já tomadas

Cada decisão aponta para a seção que explica o motivo. Se uma delas mudar, mude aqui e na seção.

| Decisão | O que ficou | Seção |
|---|---|---|
| Estilo | Camadas por módulo (monolito modular), com a dependência invertida: o domínio fica no centro e a infraestrutura depende dele, no espírito de Clean/Hexagonal | 1 |
| Dois modelos por agregado | Domínio e persistência separados, ligados por mapper | 4 |
| Referência entre agregados | No domínio, só por id; na persistência, pode ser direta (`@ManyToOne` LAZY), restrita a `infrastructure.persistence`. A alternativa (`Long kitchenId`) foi avaliada e deixada de lado | 4 e 9 |
| Dentro do mesmo agregado | Objetos diretos nos dois modelos; as partes vêm junto (`join fetch`/`@EntityGraph`) | 4 |
| Ids próprios | Cada módulo tem o seu id do vizinho (o `restaurant` tem o seu `KitchenId`) | 5 |
| Excluir cozinha com restaurantes | Quem recusa é a FK do banco (409); o `kitchen` não consulta o `restaurant`, para não criar ciclo | 5 |
| CQRS leve | Escrita via agregado e `*CommandService`; leitura via `*View` e `*QueryService`, no mesmo banco | 6 |
| Frameworks por camada | O `domain` é Java puro; a `application` pode usar `@Service` e `@Transactional`, mas não JPA nem web | 1 e 10 |
| Erros | `DomainException` vira 422; validação do Request e corpo ilegível, 400; conflito de unicidade e violação do banco, 409 | 7 |
| Enums do domínio | Ficam em `<módulo>/domain`; a `presentation` só vê `String` e a `application` converte (`from(String)` com `switch` explícito); vão para `shared/domain` só se outro módulo precisar | 10 |
| Nomes únicos | Cozinha e restaurante têm nome único, sem diferenciar maiúsculas: `existsByNameIgnoreCase` no service e `UNIQUE` no banco como última defesa | TODO 3.2 e 4.5 |
| Banco | MySQL local, sem H2; os testes que usam o banco rodam nele | 13 e TODO 1.4 |

---

## 12. Seguindo o curso

O curso usa camadas tradicionais. Para acompanhar uma aula, escreva primeiro o que o curso faz e depois traduza com
esta tabela:

| No curso (camadas tradicionais) | Neste projeto |
|---|---|
| Entidade JPA usada em todas as camadas | `Kitchen` (`domain`, Java puro) + `KitchenEntity` (`infrastructure.persistence`) + `KitchenMapper` |
| Repository do Spring Data usado no service | `KitchenRepository` (interface do domínio), implementada por `JpaKitchenRepositoryAdapter`; na leitura, `KitchenQueryRepository` |
| `@Service` com as regras e `@Transactional` | `KitchenCommandService` (escrita, `@Transactional`) e `KitchenQueryService` (leitura, `readOnly`); as regras ficam no agregado e nos value objects |
| Controller recebe e devolve a entidade | Controller recebe um `CreateKitchenRequest`, chama o service com um comando e devolve um `KitchenResponse` (pelo `KitchenWebMapper`) |
| Enum usado direto no DTO e no controller | `String` no Request e no Response; o service converte com `PaymentMethod.from(...)` (seção 10) |
| Exceção de negócio e handler | `DomainException` (422), `NotFoundException` (404), `ConflictException` (409) e o `GlobalExceptionHandler` |
| Entidade que aponta para outra (`@ManyToOne`) | No domínio, só o id; na persistência, `@ManyToOne` LAZY; a conversa entre módulos passa por `api` e `acl` (seção 5) |

**Ordem para implementar um módulo**, de dentro para fora:

1. `domain`: value objects, agregado e a interface do repositório.
2. `infrastructure.persistence`: entidade JPA, mapper, Spring Data e adapter de escrita.
3. `application`: comandos, view, exceções, port de leitura e services.
4. `infrastructure.persistence` (leitura): consulta do Spring Data e adapter de leitura.
5. `presentation`: DTOs, mapper web e controller.
6. Testes de cada camada, junto com ela.

---

## 13. Convenções do projeto

- **Git:** uma branch curta por assunto (`feat/...`, `fix/...`, `refactor/...`, `docs/...`), sempre criada a partir da
  `main` atualizada. Pull Request com merge commit, nunca squash. Mensagens em inglês, no padrão Conventional Commits
  (`tipo: resumo`; no merge do PR, `tipo: resumo (#N)`, com o corpo em inglês). Cada commit deve compilar e passar nos
  testes.
- **Comentários didáticos** nas partes importantes do código e nos testes (o que cada mock faz e por quê).
- **Perfil:** `dev`, com o MySQL local configurado em `application-dev.properties`. Os testes que usam o banco precisam
  do MySQL rodando; cada um cria os próprios dados (com nomes únicos) ou desfaz tudo no fim (o `@DataJpaTest` faz
  rollback).

---

## 14. Glossário rápido

- **Entidade de domínio**: a classe com as regras (`Kitchen`, `Restaurant`). Não tem anotações de banco.
- **Entidade JPA**: a classe espelho da tabela (`KitchenEntity`). Não tem regras.
- **Value Object**: um valor com regra própria e sem identidade (`KitchenName`, `RestaurantName`).
- **Aggregate Root**: a classe principal de um grupo; é por ela que se altera o grupo.
- **Port**: interface que o domínio ou a aplicação define para dizer "eu preciso disso".
- **Adapter**: classe da infraestrutura que implementa o port.
- **View**: objeto só de leitura, montado direto da consulta.
- **Mapper**: classe que converte um modelo em outro.
- **ACL (Anti-Corruption Layer)**: classe que traduz o vocabulário de um módulo para o de outro.
