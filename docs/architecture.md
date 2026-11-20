# Arquitetura

O projeto é um **monólito em camadas, organizado por módulo**, no estilo do curso: a entidade JPA é o modelo de
negócio, o service coordena os casos de uso e controla a transação, e o controller fala com o mundo externo por DTOs.
Este documento explica o que isso significa na prática, como as pastas estão organizadas e quais regras valem para o
código. Ele é atualizado conforme o curso avança: quando uma aula mudar a estrutura, este arquivo muda junto.

---

## 1. Monólito organizado por módulo

### Monólito
É uma aplicação só: um projeto, um `.jar`, um banco de dados. Os módulos (cozinhas, restaurantes, pagamentos) vivem
no mesmo processo e conversam por chamadas de método comuns, sem rede entre eles.

### Módulos
A divisão é feita primeiro por **área do negócio** (`kitchen`, `restaurant`, `payment`) e só depois por tipo de
classe. Tudo o que é de cozinha fica junto, no pacote `kitchen`. Assim, mexer numa funcionalidade quase nunca exige
abrir pacotes espalhados pelo projeto.

### Camadas
Dentro de cada módulo, as classes seguem as camadas tradicionais:

| Camada | Classes | Responsabilidade |
|---|---|---|
| Web | `KitchenController`, DTOs, `KitchenMapper` | Falar HTTP: receber o JSON, validar o formato e devolver a resposta |
| Negócio | `KitchenService` | Coordenar cada caso de uso e controlar a transação |
| Dados | `KitchenRepository` | Ler e gravar no banco (Spring Data) |
| Modelo | `Kitchen` (entidade JPA) | Os dados e as regras que dependem só deles |

O fluxo segue uma direção só:

```
controller  →  service  →  repository  →  banco
```

A entidade JPA é o modelo de negócio e circula entre o service e o repository. Ela **não sai pela API**: a requisição
e a resposta usam DTOs.

---

## 2. Organização das pastas

`<pacote.raiz>` é o pacote principal do projeto (`dev.denison.pedeai`):

```
src/main/java/<pacote.raiz>/
│
├── shared/                          # o que é comum a todos os módulos
│   ├── exception/                   #   BusinessException, NotFoundException, ConflictException
│   └── web/                         #   GlobalExceptionHandler
│
├── kitchen/                         # um módulo
│   ├── Kitchen                      #   entidade JPA (tb_kitchen)
│   ├── KitchenRepository            #   Spring Data (sem public)
│   ├── KitchenService               #   casos de uso e transação
│   ├── KitchenController            #   endpoints
│   ├── KitchenMapper                #   entidade -> DTO (método estático, classe e método sem public)
│   ├── dto/                         #   CreateKitchenRequest, KitchenResponse
│   └── exception/                   #   KitchenNotFoundException, KitchenNameAlreadyExistsException
│
├── restaurant/                      # outro módulo, com a mesma estrutura
│
└── payment/                         # outro módulo, com a mesma estrutura
```

Algumas observações:

- Os pacotes só são criados quando têm algo dentro. Não é preciso montar toda a árvore de uma vez.
- O repositório do Spring Data e o mapper ficam **sem `public`** (visíveis só dentro do pacote do módulo). Assim, o
  compilador impede que outro módulo os use: de fora, só se enxerga a entidade e o service.
- Só vai para o `shared` o que mais de um módulo usa.
- Os testes seguem a mesma estrutura de pacotes em `src/test/java`.

---

## 3. Onde ficam as regras

Cada tipo de regra tem o seu lugar:

| Onde | O que confere | Se falhar |
|---|---|---|
| Request (controller) | O **formato**: campo vazio, tamanho (`@NotBlank`, `@Size`), com mensagem por campo | 400 |
| Entidade | O que depende **só dos próprios dados**: normalizar e validar o nome, mudar de status | 422 (`BusinessException`) |
| Service | O que precisa do **banco ou de outros objetos**: nome já em uso, cozinha existente | 409 ou 404 |
| Banco | A última defesa: `NOT NULL`, tamanho e `UNIQUE` (vale mesmo numa corrida entre duas requisições) | 409 (o `UNIQUE` do nome vira `KitchenNameAlreadyExistsException`; os demais, mensagem genérica) |

**Corrida entre duas requisições.** Conferir `existsByNameIgnoreCase` e depois salvar não basta: duas requisições podem
ver o nome livre antes de qualquer commit. Quem decide é a constraint `UNIQUE` (`uk_kitchen_name`). O service salva com
`saveAndFlush`, para o INSERT acontecer dentro do método, e, se o banco recusar por essa constraint, lança a mesma
`KitchenNameAlreadyExistsException` da checagem. Só a violação dessa constraint é traduzida: o service a reconhece pelo
nome (o MySQL o informa com o prefixo da tabela, `tb_kitchen.uk_kitchen_name`), e as outras violações de integridade
seguem como estão e viram o 409 genérico. A checagem continua útil: no caso comum devolve o 409 sem nem tentar o INSERT.
Se um dia o banco mudar, o `KitchenServiceDatabaseTest` mostra se o nome continua sendo reconhecido.

As mensagens do Bean Validation ficam em `src/main/resources/messages.properties`: cada anotação aponta para a sua
chave (`@NotBlank(message = "{kitchen.name.required}")`), e o Spring Boot lê o arquivo sem configuração extra. O
`@Size` usa as constantes da entidade (`Kitchen.NAME_MIN_LENGTH` e `NAME_MAX_LENGTH`), então o limite do Request, o da
entidade e o da coluna são o mesmo número.

O Request tira os espaços das pontas do nome (`trim()`, o mesmo da entidade) antes de validar, num construtor compacto
do record. Assim o Request e a entidade medem o mesmo texto: `"  a"` e `"a"` são recusados da mesma forma (400), e um
nome de 60 caracteres com espaço nas pontas é aceito pelos dois. Um erro de tamanho vindo de uma requisição é sempre
400. A checagem da entidade continua valendo para quem não passa pelo Request (outros módulos e testes). Um nome só de
espaços vira `""` e falha em `@NotBlank` e em `@Size`, então a resposta traz as duas mensagens.

O id da URL (`GET /kitchens/{id}`) não leva `@Positive` nem `@NotNull`: ele nunca chega nulo, e um id que não existe,
inclusive `0` ou negativo, já vira 404 pelo service. Se um parâmetro do controller ganhar uma constraint (um `@Min` num
`@RequestParam` de paginação, por exemplo), a mensagem também fica no `messages.properties`.

A entidade não expõe `setters` para os campos que têm regra: a mudança passa por um método com nome de negócio.

Ela também não tem construtor público: nasce por uma fábrica com nome. `Kitchen.create(name)` cria uma cozinha nova,
ainda sem id, e valida o nome. `Kitchen.restore(id, name)` monta uma cozinha que já tem id (por exemplo, nos testes dos
services) e exige um id positivo. O Hibernate usa só o construtor sem argumentos.

---

## 4. Relação entre módulos

Um módulo pode usar de outro a **entidade** (para a associação JPA) e o **service**. Não usa o repository, o controller
nem os DTOs dele. A dependência tem **uma direção só** e não pode formar ciclo: o `restaurant` conhece o `kitchen`, e
o `kitchen` não conhece o `restaurant`.

```java
@Entity
@Table(name = "tb_restaurant")
public class Restaurant {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kitchen_id", foreignKey = @ForeignKey(name = "fk_restaurant_kitchen"))
    private Kitchen kitchen;

    private String name;
}
```

- **Validar a cozinha:** ao criar um restaurante, o `RestaurantService` chama `KitchenService.findById(kitchenId)`, que
  lança `KitchenNotFoundException` (404) se ela não existir. O `restaurant` não consulta o `KitchenRepository`.
- **Excluir uma cozinha que tem restaurantes:** a chave estrangeira do banco recusa o `DELETE`, e o
  `GlobalExceptionHandler` traduz a `DataIntegrityViolationException` em HTTP 409. O `kitchen` não consulta o
  `restaurant` (seria um ciclo), e a FK é a única garantia que não perde a corrida contra um restaurante criado no
  mesmo instante. O custo é a mensagem do 409 ser genérica.

### Cuidados com `LAZY`
- **Sempre `LAZY`** nas associações `@ManyToOne`. O padrão do JPA é `EAGER`, que carregaria a cozinha junto com cada
  restaurante, mesmo sem ninguém precisar.
- O projeto usa `spring.jpa.open-in-view=false`. Fora da transação, tocar numa associação `LAZY` que não foi carregada
  lança `LazyInitializationException`. Por isso a consulta que alimenta uma resposta traz a associação junto
  (`join fetch` ou `@EntityGraph`), e a conversão para DTO só usa o que foi carregado. `getKitchen().getId()` não
  precisa de consulta; `getKitchen().getName()` precisa.
- Índice na coluna da FK (`@Table(indexes = ...)`): nem todo banco cria índice para FK sozinho (o PostgreSQL não cria).

---

## 5. Exemplo de ponta a ponta

`POST /kitchens`:

1. O **controller** recebe o JSON, valida o formato (`@Valid`) e chama o service com o nome.
2. O **service** cria a `Kitchen` com `Kitchen.create(name)` (a entidade tira os espaços e valida o nome), confere
   `existsByNameIgnoreCase` com o nome já normalizado e salva pelo repository (`saveAndFlush`), dentro da transação.
3. O **banco** é a última defesa: `UNIQUE` e `NOT NULL`. Se duas requisições passarem juntas pela checagem, o `UNIQUE`
   barra a segunda, e o service traduz a violação em `KitchenNameAlreadyExistsException` (409).
4. O **controller** converte a entidade em `KitchenResponse` pelo `KitchenMapper` e responde 201 com `Location`.

`POST /restaurants` tem os mesmos passos, com uma pergunta a mais no passo 2: o service busca a cozinha com
`KitchenService.findById`. Se não existir, a exceção vira 404.

### Como os erros viram HTTP
O `GlobalExceptionHandler` (em `shared/web`) devolve tudo como `ProblemDetail`:

| Exceção | Onde nasce | HTTP |
|---|---|---|
| `BusinessException` | entidade (regra de negócio) | 422 |
| `NotFoundException` (`KitchenNotFoundException`...) | service | 404 |
| `ConflictException` (`KitchenNameAlreadyExistsException`...) | service | 409 |
| `DataIntegrityViolationException` (FK, `NOT NULL`...; o `UNIQUE` do nome o service já traduz em `KitchenNameAlreadyExistsException`) | banco | 409, com mensagem genérica |
| `MethodArgumentNotValidException` (`@Valid` no Request) | controller | 400, com os erros por campo (`errors`) |
| `HandlerMethodValidationException` (constraint de parâmetro, como um `@Min` num `@RequestParam`) | controller | 400, com os erros por parâmetro (`errors`) |
| `HttpMessageNotReadableException` (JSON malformado, corpo ausente, valor de enum desconhecido) | controller | 400, com mensagem genérica |

A mensagem do banco (SQL, nome de constraint) e a do Jackson (nomes internos de classe) nunca vão para o cliente: a
resposta traz um texto fixo.

O 400 de validação traz a lista `errors`, com um item por erro. Um campo pode ter mais de um (um `name` vazio falha em
`@NotBlank` e em `@Size`), e a lista sai ordenada por campo e mensagem. As constraints de parâmetro usam o mesmo
formato, com o nome do parâmetro em `field`:

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/kitchens",
  "errors": [
    { "field": "name", "message": "kitchen name is required" },
    { "field": "name", "message": "kitchen name must have between 2 and 60 characters" }
  ]
}
```

O handler estende `ResponseEntityExceptionHandler`, então os erros do próprio Spring MVC (método não permitido, tipo de
mídia não aceito, id que não é número, rota inexistente) também saem como `ProblemDetail`.

O `GlobalExceptionHandlerTest` fixa o essencial desse contrato (status e corpo dos casos principais, a ordem de
`errors` e a ausência das mensagens do banco e do Jackson). Ele usa um controller de teste, porque nenhum endpoint real
tem constraint de parâmetro.

---

## 6. Enums

Um enum que descreve o negócio (forma de pagamento, status) mora no pacote do módulo dono dele:
`payment/PaymentMethod`. Ele é usado direto na entidade, no DTO e no controller.

- **Na entidade:** `@Enumerated(EnumType.STRING)`, nunca `ORDINAL` (a ordem das constantes pode mudar).
- **No DTO:** o próprio enum. O Jackson converte o texto do JSON, e um valor desconhecido vira
  `HttpMessageNotReadableException` (400). O springdoc lista os valores sozinho.
- **O que fica acoplado:** o valor da API é o nome da constante, e o banco guarda esse nome. Renomear uma constante
  muda a saída da API e exige migração do banco. O teste de API fixa os valores do JSON.
- **Outro módulo:** se o `restaurant` precisar do `PaymentMethod`, usa o do `payment` (a dependência segue a direção
  da seção 4). O enum só vai para o `shared` se os dois módulos precisarem dele e isso formaria um ciclo.

---

## 7. Regras

- o controller chama só o service do próprio módulo e o mapper, nunca o repository;
- a transação fica no service;
- o repository só é usado pelo service do próprio módulo;
- a entidade JPA não aparece no corpo da requisição nem da resposta: a API usa DTOs;
- um módulo usa de outro só a entidade e o service;
- não há ciclos entre módulos;
- cada tipo de classe está no pacote certo (controllers, services, entidades e DTOs do módulo).

Por enquanto essas regras valem por convenção e por revisão do código. No futuro, um teste de arquitetura com o
**ArchUnit** poderá conferi-las automaticamente a cada build (item 7.7 do `TODO.md`).

---

## 8. Custos desta escolha

- **A entidade JPA é também o modelo de negócio.** Anotações de banco e regras convivem na mesma classe. As regras
  continuam testáveis sem subir o Spring (a entidade é uma classe comum), mas o JPA fica no classpath do teste.
- **Os módulos se acoplam pelas entidades e pelo schema.** O `Restaurant` aponta para a `Kitchen`; mudar uma entidade
  pode afetar o módulo vizinho.
- **O mapeamento entre entidade e DTO é manual.**

---

## 9. Seguindo o curso

O curso usa camadas tradicionais, e este projeto fica perto dele. As diferenças:

| No curso | Neste projeto |
|---|---|
| Pacotes por camada | Pacotes por módulo (`kitchen`, `restaurant`, `payment`), com as classes de cada um juntas |
| Controller recebe e devolve a entidade | Controller recebe um `CreateKitchenRequest` e devolve um `KitchenResponse` (pelo `KitchenMapper`) |
| Exceção de negócio e handler | `BusinessException` (422), `NotFoundException` (404), `ConflictException` (409) e o `GlobalExceptionHandler` |

O resto é igual: entidade JPA nos services e nos repositories, `@Service` com `@Transactional`, repository do Spring
Data, enum direto no DTO e `@ManyToOne` entre entidades.

**Ordem para implementar um módulo**, de dentro para fora:

1. Entidade, com testes de unidade (JUnit puro, sem Spring).
2. Repository, com teste `@DataJpaTest`.
3. Exceções e service, com testes usando Mockito.
4. DTOs, mapper e controller, com testes de API (`MockMvc`).

---

## 10. Convenções do projeto

- **Git:** uma branch curta por assunto (`feat/...`, `fix/...`, `refactor/...`, `docs/...`), sempre criada a partir da
  `main` atualizada. Pull Request com merge commit, nunca squash. Mensagens em inglês, no padrão Conventional Commits
  (`tipo: resumo`; no merge do PR, `tipo: resumo (#N)`, com o corpo em inglês). Cada commit deve compilar e passar nos
  testes.
- **Perfil:** `dev`, com o MySQL local configurado em `application-dev.properties`. Os testes que usam o banco precisam
  do MySQL rodando; cada um cria os próprios dados (com nomes únicos) ou desfaz tudo no fim (o `@DataJpaTest` faz
  rollback). Atenção: o `dev` usa `ddl-auto=drop-and-create`, então cada teste que sobe o contexto recria o schema e
  apaga os dados de desenvolvimento (o `import.sql` repõe só as cozinhas de exemplo). Para rodar os testes sem mexer
  nesses dados, aponte o datasource para outro banco (o `createDatabaseIfNotExist=true` o cria na primeira conexão):
  `SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/pedeai_test?createDatabaseIfNotExist=true&serverTimezone=UTC'
  ./mvnw clean test`. O item 6.6 do `TODO.md` resolve isso de vez com Testcontainers.

---
