# TODO

Lista de trabalho do projeto. Os itens estão na ordem em que devem ser feitos, e o arquivo vai sendo atualizado
conforme o curso avança (itens novos entram, os concluídos são marcados).

A arquitetura, as regras e as convenções estão em [`architecture.md`](architecture.md).

Legenda: `[ ]` pendente · `[x]` concluído · 🔴 prioridade alta · 🟠 média · 🟢 baixa

---

## 1. Criar o projeto

- [x] **1.1** 🔴 Gerar o projeto no Spring Initializr (Maven, Java 21) com: Spring Web, Spring Data JPA, MySQL Driver
  e Validation.
- [x] **1.2** Criar o repositório no GitHub (vazio, sem README).
- [x] **1.3** Configurar `application.properties`: nome da aplicação e o perfil fixo (`dev`). Criar `application-dev.properties`.
- [x] **1.4** Adicionar o `architecture.md` e este `TODO.md` em `docs/`.
- [x] **1.5** Primeiro commit na `main` e push: `chore: initialize project`.

---

## 2. Base compartilhada (`shared`)

Antes da criação do primeiro módulo:

- [x] **2.1** `shared/exception`: `BusinessException` (erro de regra de negócio, vira HTTP 422), `NotFoundException`
  (base das exceções "não encontrado", vira HTTP 404) e `ConflictException` (base das exceções de conflito, vira
  HTTP 409). A `BusinessException` também traz os métodos estáticos `requireNonNull` e `requirePositive`, para
  validar argumentos nas entidades.

---

## 3. Primeiro módulo: `kitchen`

Fazer **de dentro para fora**: entidade, repository, service, web (`architecture.md`, seção 9).

O `Kitchen` tem só nome. É de propósito: o primeiro módulo serve para aprender o roteiro, que os outros repetem.

### 3.1 Entidade e repository (`kitchen`)
- [x] **3.1.1** `Kitchen`: entidade JPA (`tb_kitchen`) com `id` e `name`. Sem construtor público: nasce por
  `create(name)` (nova, sem id) ou `restore(id, name)` (com id positivo).
- [x] **3.1.2** Testes de unidade da entidade (JUnit puro, sem Spring), só os essenciais: nome válido com os espaços
  das pontas aparados, os limites de 2 e 60 caracteres (medidos depois de aparar), nome nulo, curto demais e longo
  demais recusados, e `changeName` (inclusive que um nome recusado não altera o anterior).
- [x] **3.1.3** `KitchenRepository` (`JpaRepository`, sem `public`), com `existsByNameIgnoreCase`.
- [x] **3.1.4** `import.sql` com algumas cozinhas de exemplo.
- [x] **3.1.5** Teste `@DataJpaTest` (com `@AutoConfigureTestDatabase(replace = NONE)`, para usar o MySQL configurado
  em vez de trocá-lo por um banco em memória): o banco recusar nome duplicado (a última linha de defesa, mesmo que a
  checagem do service falhe numa corrida entre duas requisições; o service traduz essa violação, item 3.2.2). No
  MySQL, com a collation padrão (`utf8mb4_0900_ai_ci`), a constraint UNIQUE também ignora a caixa (`ITALIANA` é
  recusado se `Italiana` existe). A regra "sem diferenciar a caixa" não depende disso: é do `existsByNameIgnoreCase` do
  service, e num banco com collation sensível à caixa a constraint barraria só o nome idêntico. O teste também confere
  o `existsByNameIgnoreCase`, que ignora a caixa.

### 3.2 Service (`kitchen`)
- [x] **3.2.1** `exception/KitchenNotFoundException` (herda `NotFoundException`) e
  `exception/KitchenNameAlreadyExistsException` (herda `ConflictException`). Cada uma recebe o valor (o `id` ou o
  nome) e monta a própria mensagem.
- [x] **3.2.2** `KitchenService` (`@Transactional`; as leituras com `readOnly = true`): `create(String name)` (cria a
  `Kitchen` com `Kitchen.create(name)`, confere `existsByNameIgnoreCase` com o nome já normalizado e salva com
  `saveAndFlush`), `findById` (404 se não existir) e `findAll`. Os métodos devolvem a entidade. A checagem sozinha não
  fecha a corrida entre duas requisições (as duas podem ver o nome livre), então o `create` também captura a
  `DataIntegrityViolationException` do INSERT e, se a causa for a constraint `uk_kitchen_name` (`Kitchen.UNIQUE_NAME`,
  reconhecida pelo nome, que o MySQL informa como `tb_kitchen.uk_kitchen_name`), lança a mesma
  `KitchenNameAlreadyExistsException`. As outras violações de integridade seguem adiante, para não chamar de "nome em
  uso" o que é outro problema.
- [x] **3.2.3** Testes do service, só os essenciais. Com Mockito (`KitchenServiceTest`): criar (conferindo o nome
  aparado), nome já em uso (nada é salvo), a violação UNIQUE do nome virando "nome em uso", as outras violações de
  integridade seguindo como estão e cozinha inexistente. No MySQL real (`KitchenServiceDatabaseTest`, `@DataJpaTest`):
  perder a corrida (a checagem diz "livre", mas o nome já existe) vira `KitchenNameAlreadyExistsException`, o que prova
  que a violação que o MySQL de fato devolve é reconhecida.

### 3.3 Web (`kitchen`)
- [x] **3.3.1** `dto/CreateKitchenRequest` e `dto/KitchenResponse`, com Bean Validation nos campos de entrada
  (`@NotBlank`, `@Size`). Os limites de tamanho são as constantes da entidade (`Kitchen.NAME_MIN_LENGTH` e
  `NAME_MAX_LENGTH`, as mesmas da coluna). As mensagens ficam em `messages.properties` (o Spring Boot o lê no Bean
  Validation sem configuração extra), e cada anotação aponta para a sua chave (`message = "{kitchen.name.required}"`).
  O `CreateKitchenRequest` apara o nome (`trim()`, como a entidade) num construtor compacto, então as anotações medem o
  mesmo texto que a entidade mede. Sem teste próprio: os testes de API (3.3.6) cobrem o aparo e o texto das mensagens.
- [x] **3.3.2** `KitchenMapper` (classe utilitária e método estático `toResponse`, os dois sem `public`): entidade →
  `KitchenResponse`. Sem teste próprio: os testes de API (3.3.6) conferem o `id` e o `name` da resposta.
- [x] **3.3.3** `GlobalExceptionHandler` (em `shared/web`): traduz `BusinessException` (422), `NotFoundException` (404),
  `ConflictException` (409), `MethodArgumentNotValidException` (400, com os erros por campo),
  `HandlerMethodValidationException` (400, constraint de parâmetro do controller, como um `@Min` num `@RequestParam`,
  com o nome do parâmetro em `field`), `HttpMessageNotReadableException` (400, JSON malformado, corpo ausente ou valor
  de enum desconhecido, com mensagem genérica) e `DataIntegrityViolationException` (409, com mensagem genérica) em
  respostas HTTP padronizadas (`ProblemDetail`). A mensagem do Jackson cita nomes internos de classe e a do banco
  expõe o SQL: nenhuma das duas vai para o cliente. Um campo pode ter mais de um erro (um `name` vazio falha em
  `@NotBlank` e em `@Size`), então os erros por campo são uma lista de `{field, message}` (propriedade `errors`), e não
  um mapa de campo para mensagem. A ordem do Bean Validation não é garantida, então a lista sai ordenada por campo e
  mensagem. O handler estende `ResponseEntityExceptionHandler`, então os erros do próprio Spring MVC (405, 415, id que
  não é número, rota inexistente) também saem como `ProblemDetail`.
- [x] **3.3.4** Teste do `GlobalExceptionHandler` (`GlobalExceptionHandlerTest`, `@WebMvcTest` com um controller de
  teste dentro da própria classe, sem depender de nenhum módulo), só o contrato essencial: status e corpo de 422, 404 e
  409, o 409 do banco sem a mensagem dele, os 400 de validação do corpo (com a ordem da lista `errors`, por campo e
  depois por mensagem), de constraint de parâmetro (`field` com o nome do parâmetro, inclusive junto com um `@Valid` no
  corpo) e de corpo ilegível (sem a mensagem do Jackson), e um erro do próprio Spring MVC (id que não é número) saindo
  como `ProblemDetail`. O controller de teste existe porque nenhum endpoint atual tem constraint de parâmetro.
- [x] **3.3.5** `KitchenController`: `POST /kitchens` (responde 201 com `Location`, que aponta para
  `/kitchens/{id}`), `GET /kitchens/{id}` e `GET /kitchens`. O id da URL não tem `@Positive`: um id que não
  existe, inclusive `0` ou negativo, já vira 404 pelo service.
- [x] **3.3.6** Testes de API com `MockMvc` (`KitchenControllerTest`, `@WebMvcTest` com o `KitchenService` mockado),
  só os essenciais: `POST` com 201, `Location` (com o `/kitchens`) e o nome aparado; 400 de nome em branco (as duas
  mensagens do `messages.properties`, em ordem estável); 409 de nome duplicado; `GET /kitchens/{id}` com 200 e 404
  (ids `99` e `0`); e `GET /kitchens` com a lista. O mapeamento de cada exceção em HTTP (inclusive o 422) é fixado
  no `GlobalExceptionHandlerTest`. O `@WebMvcTest` carrega o `GlobalExceptionHandler` e o `messages.properties`, sem
  banco.

### 3.4 Fechamento
- [x] **3.4.1** Rodar `./mvnw clean test` e conferir que tudo passa. O `dev` recria o schema a cada
  teste que sobe o contexto; para não apagar os dados de desenvolvimento, rode apontando `SPRING_DATASOURCE_URL` para
  outro banco (`architecture.md`, seção 10).
- [ ] **3.4.2** Abrir o PR do módulo (`feat/kitchen-module`) e fazer o merge com merge commit.

### 3.5 Para depois
- [ ] **3.5.1** Alterar cozinha (`PUT /kitchens/{id}`) e remover (`DELETE /kitchens/{id}`). A exclusão de uma cozinha
  que tem restaurantes é recusada pela FK e vira 409 (`architecture.md`, seção 4); o teste disso vem no item 4.7.

---

## 4. Módulo `restaurant`

Segundo módulo e o primeiro que **usa outro**: cada passo abaixo existe por causa dessa relação. Mesmo roteiro da
seção 3, com os acréscimos da seção 4 do `architecture.md` (associação com a `Kitchen` e uso do `KitchenService`).

- [ ] **4.1** `Restaurant`: entidade JPA (`tb_restaurant`) com `id`, `name` e `kitchen`. Detalhes da persistência:
  - `@ManyToOne(fetch = LAZY, optional = false)` para `Kitchen`, com a FK nomeada (`fk_restaurant_kitchen`) criada
    pelo mapeamento;
  - índice em `tb_restaurant.kitchen_id` (`@Table(indexes = ...)`), com nome (`idx_restaurant_kitchen`), porque nem
    todo banco cria índice para FK sozinho (o PostgreSQL não cria);
  - `UNIQUE` em `name`, com nome (`uk_restaurant_name`).

  Regras da entidade: `create(...)`, `changeName(...)` e `changeKitchen(...)`, com a mesma normalização do nome da cozinha.
  Testes de unidade (JUnit puro).
- [ ] **4.2** `RestaurantRepository` (sem `public`), com `existsByNameIgnoreCase` e consultas que trazem a cozinha junto
  (`join fetch` ou `@EntityGraph`) para buscar por id, listar e listar por cozinha. Teste `@DataJpaTest`: ida e volta
  do restaurante **fora de transação**, lendo `kitchen.name` sem `LazyInitializationException`; FK e índice lidos do
  schema real.
- [ ] **4.3** `exception/RestaurantNotFoundException` e `exception/RestaurantNameAlreadyExistsException`. A cozinha
  inexistente usa a `KitchenNotFoundException` do `kitchen` (404).
- [ ] **4.4** `RestaurantService` (leitura): `findById`, `findAll` e `findAllByKitchen(kitchenId)`, que confere a cozinha com
  `KitchenService.findById` (404 se ela não existir). Testes com Mockito.
- [ ] **4.5** Web: `dto/RestaurantResponse` (com o id e o nome da cozinha), `RestaurantMapper` e `RestaurantController`
  (`GET /restaurants`, `GET /restaurants/{id}`, `GET /kitchens/{id}/restaurants`).
- [ ] **4.6** Testes de API dos endpoints de leitura, incluindo 404 de restaurante e de cozinha inexistentes.
- [ ] **4.7** Escrita: `create` (busca a cozinha com `KitchenService.findById` e confere o nome único), `changeName` e
  `changeKitchen` no `RestaurantService`, com os endpoints correspondentes, o DTO de entrada e os testes. Teste:
  excluir uma cozinha que tem restaurante devolve 409 e a cozinha continua existindo (depende do item 3.5.1).

---

## 5. Módulo `payment`

Terceiro módulo. Segue o roteiro da seção 3, de dentro para fora. Os campos do `Payment` são os que a aula definir;
ele ainda não se liga a `restaurant` (item 7.4).

- [x] **5.1** Decisão tomada: os enums do `Payment` (forma de pagamento, status) ficam em `payment` (por exemplo
  `PaymentMethod`, ao lado do `Payment`) e são usados direto na entidade (`@Enumerated(EnumType.STRING)`) e nos DTOs.
  Um valor desconhecido no JSON vira 400. Os motivos estão em `architecture.md`, seção 6.
- [ ] **5.2** `Payment`: entidade JPA (`tb_payment`) com os campos da aula e os enums. Testes de unidade das regras da
  entidade.
- [ ] **5.3** `PaymentRepository` e teste `@DataJpaTest` (o enum é gravado como texto).
- [ ] **5.4** `exception/PaymentNotFoundException` e `PaymentService`, com testes usando Mockito.
- [ ] **5.5** Web: DTOs (com os enums), `PaymentMapper` e `PaymentController` (`POST /payments`,
  `GET /payments/{id}`, `GET /payments`). Testes de API, incluindo o valor de enum desconhecido (400) e os valores do
  JSON de cada enum.
- [ ] **5.6** Rodar `./mvnw clean test` e abrir o PR do módulo (`feat/payment-module`).

---

## 6. Qualidade e acabamento

- [ ] **6.1** 🟠 `README.md` na raiz: o que é o projeto, como rodar e link para `docs/architecture.md`.
- [ ] **6.2** 🟢 Teste de fumaça (`@SpringBootTest` com `MockMvc`) percorrendo o ciclo de vida de cada módulo pelo HTTP.
- [ ] **6.3** 🟢 Documentação da API (springdoc-openapi).
- [ ] **6.4** 🟢 Migrations (Flyway) no lugar do schema gerado pelo Hibernate + `import.sql`.
- [ ] **6.5** 🟢 Controle otimista (`@Version`) nas entidades alteradas com frequência (restaurantes, pagamentos).
- [ ] **6.6** 🟢 Testcontainers (MySQL) nos testes que usam o banco, para não depender do MySQL local nem mexer nos
  dados de desenvolvimento.

---

## 7. Para depois

Itens adiados de propósito, fora do escopo por enquanto.

- [ ] **7.1** Paginação (`Pageable`) nas listagens, com limite de tamanho e ordenação permitida.
- [ ] **7.2** Autenticação e autorização (OAuth2/JWT). Ao chegar aqui:
  - remover o perfil `dev` fixo do `application.properties` e ativar por `SPRING_PROFILES_ACTIVE`;
  - parar de tratar todos os endpoints como públicos.
- [ ] **7.3** Ordem determinística nas listagens (`findAll` sem `ORDER BY` devolve as linhas em ordem qualquer).
- [ ] **7.4** Ligar `Payment` a `Restaurant` (por exemplo, validar que o restaurante aceita a forma de pagamento):
  segue o roteiro da seção 4 (associação JPA e uso do service do vizinho). O `restaurant` passa a depender do
  `payment`, e o `payment` não pode conhecer o `restaurant` (sem ciclos). O `restaurant` usa o `PaymentMethod` do
  `payment`.
- [ ] **7.5** Demais entidades e módulos: seguir o roteiro da seção 3, respeitando as regras da seção 7 do
  `architecture.md`.
- [ ] **7.6** Validações personalizadas e demais tópicos conforme surgirem.
- [ ] **7.7** Teste de arquitetura com ArchUnit (dependência `archunit-junit5`, escopo `test`), para travar sozinho as
  regras da seção 7 do `architecture.md`: classes `*Controller` não usam `*Repository`; `*Repository` só é usado por
  `*Service` do mesmo módulo; entidades JPA não aparecem nos DTOs nem no corpo das respostas; módulos sem ciclos
  (`kitchen` não conhece `restaurant`). Cada regra com `.because("...")`. Até lá, as regras valem por convenção e
  revisão. Vale adicionar quando já houver dois módulos, e provar cada regra provocando uma violação de propósito.
