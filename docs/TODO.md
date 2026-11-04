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

- [ ] **2.1** `domain`: `DomainException` (para erros de regras de negócio, vira HTTP 422).
- [ ] **2.2** `application`: `NotFoundException` (base das exceções "não encontrado", vira HTTP 404) e
  `ConflictException` (base das exceções de conflito, vira HTTP 409).

---

## 3. Primeiro módulo: `kitchen`

Fazer **de dentro para fora**: domínio, persistência, aplicação, web.

### 3.1 Domínio (`kitchen/domain`)
- [ ] **3.1.1** `vo/KitchenId` (record com `Long`, não aceita nulo nem valor menor que 1).
- [ ] **3.1.2** `vo/KitchenName` (record com `String`), com validação e normalização: obrigatório, sem espaços nas
  pontas e com tamanho mínimo e máximo, medidos depois de tirar os espaços. Os erros lançam `DomainException`.
- [ ] **3.1.3** `AggregateRoot` (em `shared/domain`): classe base opcional dos agregados; pode ficar para quando houver
  uso real.
- [ ] **3.1.4** `Kitchen`: o agregado, com `create(...)` (novo, valida tudo) e `restore(...)` (vindo do banco).
  Contém métodos de negócio (ex.: `changeName`).
- [ ] **3.1.5** `KitchenRepository`: interface de escrita (`save`, `findById`, `existsByName`).
- [ ] **3.1.6** Testes de unidade do domínio (JUnit puro, sem Spring): value objects e `Kitchen`.

### 3.2 Persistência (`kitchen/infrastructure/persistence`)
- [ ] **3.2.1** `KitchenEntity`: entidade JPA (`tb_kitchen`), só colunas, anotações e GETTERs/SETTERs.
- [ ] **3.2.2** `KitchenMapper`: `toEntity(Kitchen)` e `toDomain(KitchenEntity)`.
- [ ] **3.2.3** `SpringDataKitchenRepository` (`JpaRepository`, sem `public`), com `existsByNameIgnoreCase`.
- [ ] **3.2.4** `JpaKitchenRepositoryAdapter`: implementa `KitchenRepository` usando o mapper.
- [ ] **3.2.5** `import.sql` com algumas cozinhas de exemplo.
- [ ] **3.2.6** Teste de que o banco recusa nome duplicado (a última linha de defesa, mesmo que a checagem do service
  falhe numa corrida entre duas requisições). A constraint só barra nome idêntico; a regra "sem diferenciar a caixa"
  é do `existsByNameIgnoreCase` do service.

### 3.3 Aplicação (`kitchen/application`)
- [ ] **3.3.1** `view/KitchenView`: record de leitura, com um `from(Kitchen)` para o service montar a resposta de
  escrita a partir do agregado salvo.
- [ ] **3.3.2** `command/CreateKitchenCommand`.
- [ ] **3.3.3** `exception/KitchenNotFoundException` (herda `NotFoundException`) e `KitchenNameAlreadyExistsException`
  (herda `ConflictException`).
- [ ] **3.3.4** `port/KitchenQueryRepository`: interface de leitura (devolve `KitchenView`).
- [ ] **3.3.5** `KitchenCommandService` (`@Transactional`): criar cozinha, conferindo `existsByName` antes de salvar, e
  devolver o `KitchenView`.
- [ ] **3.3.6** `KitchenQueryService` (`@Transactional(readOnly = true)`): buscar por id e listar.
- [ ] **3.3.7** Testes de unidade dos services com Mockito.

### 3.4 Leitura na persistência
- [ ] **3.4.1** `SpringDataKitchenQueryRepository`: estende `Repository<>` (não `JpaRepository`) e projeta direto em
  `KitchenView` com `select new`. Nenhum método com nome do CRUD (`findById`, `findAll`, `existsById`): use
  `findViewById` (`architecture.md`, seção 6).
- [ ] **3.4.2** `JpaKitchenQueryRepositoryAdapter`: implementa `KitchenQueryRepository`.
- [ ] **3.4.3** Teste (`@DataJpaTest` com `@AutoConfigureTestDatabase(replace = NONE)`, para usar o MySQL configurado
  em vez de trocá-lo por um banco em memória) de que as consultas devolvem `KitchenView` de verdade (por id, por lista e
  id inexistente).

### 3.5 Web (`kitchen/presentation`)
- [ ] **3.5.1** `dto/CreateKitchenRequest` e `dto/KitchenResponse`, com Bean Validation nos campos de entrada
  (`@NotBlank`, `@Size`). Os limites de tamanho devem bater com os do `KitchenName` e os da coluna.
- [ ] **3.5.2** `KitchenWebMapper`: DTO ↔ comando/view.
- [ ] **3.5.3** `GlobalExceptionHandler` (em `shared/presentation`): traduz `DomainException` (422),
  `NotFoundException` (404), `ConflictException` (409), `MethodArgumentNotValidException` (400, com os erros por
  campo), `HttpMessageNotReadableException` (400, JSON malformado ou corpo ausente, com mensagem genérica)
  e `DataIntegrityViolationException` (409, com mensagem genérica) em respostas HTTP padronizadas (`ProblemDetail`).
  A mensagem do Jackson cita nomes internos de classe e a do banco expõe o SQL: as duas vão só para o log.
- [ ] **3.5.4** Teste do `GlobalExceptionHandler`, fixando o contrato de erros (status e corpo de cada caso).
- [ ] **3.5.5** `KitchenController`: `POST /kitchens` (responde 201 com `Location`), `GET /kitchens/{id}` e
  `GET /kitchens`.
- [ ] **3.5.6** Testes de API com `MockMvc` (status, headers e JSON de cada endpoint, incluindo os erros: 400 de
  validação, 404 de cozinha inexistente, 409 de nome duplicado, 422 de regra de domínio, por exemplo um nome que só
  tem 1 caractere depois de tirar os espaços).

### 3.6 Fechamento
- [ ] **3.6.1** Rodar `./mvnw clean test` e conferir que tudo passa.
- [ ] **3.6.2** Abrir o PR do módulo (`feat/kitchen-module`) e fazer o merge com merge commit.

### 3.7 Para depois
- [ ] **3.7.1** Alterar cozinha (`PUT /kitchens/{id}`) e remover (`DELETE /kitchens/{id}`). A exclusão de uma cozinha
  que tem restaurantes é recusada pela FK e vira 409 (`architecture.md`, seção 5); o teste disso vem no item 4.8.

---

## 4. Módulo `restaurant`

Segundo módulo e o primeiro que **conversa com outro**: cada passo abaixo existe por causa dessa conversa.
Mesmo roteiro da seção 3, com os acréscimos de comunicação entre módulos.

- [ ] **4.1** `kitchen/api`: `KitchenModuleApi` com `existsById(Long)`, implementada por um facade na `application` de
  `kitchen`. É o único pacote de `kitchen` que `restaurant` poderá enxergar.
- [ ] **4.2** `restaurant/domain`: `RestaurantId`, `KitchenId` (o `restaurant` tem o **seu próprio** id de cozinha,
  sem reaproveitar o do `kitchen`; mesmo nome, outro pacote), `RestaurantName`, `Restaurant` (`create`, `restore`,
  `changeName`, `changeKitchen`) e `RestaurantRepository`. Testes de unidade do domínio.
- [ ] **4.3** `restaurant/application`: port `KitchenLookup` (`exists(KitchenId)`), `RestaurantView`,
  `RestaurantQueryRepository`, `RestaurantQueryService`, exceções `RestaurantNotFoundException`,
  `KitchenNotFoundException` (a do `restaurant`, para o 404 de cozinha inexistente) e
  `RestaurantNameAlreadyExistsException`.
- [ ] **4.4** `restaurant/infrastructure/acl`: `KitchenModuleKitchenLookup`, que implementa `KitchenLookup` chamando
  `kitchen.api`.
- [ ] **4.5** `restaurant/infrastructure/persistence`:
  - `RestaurantEntity` com `@ManyToOne(fetch = LAZY)` para `KitchenEntity` e a FK com nome (`fk_restaurant_kitchen`)
    criada pelo mapeamento, mais índice em `tb_restaurant.kitchen_id` (`@Table(indexes = ...)`), com nome
    (`idx_restaurant_kitchen`), porque nem todo banco cria índice para FK sozinho (o PostgreSQL não cria);
    `UNIQUE` em `name`, com nome (`uk_restaurant_name`);
  - `RestaurantMapper` recebendo a `KitchenEntity` de referência, e adapter criando-a com
    `entityManager.getReference(KitchenEntity.class, id)` (o `save` deve ser `@Transactional`);
  - consultas de leitura usando `r.kitchen.id`, que não faz join (lembrar dos nomes de método, item 3.4.1).
- [ ] **4.6** `restaurant/presentation`: `RestaurantResponse`, `RestaurantWebMapper` e `RestaurantController`
  (`GET /restaurants`, `GET /restaurants/{id}`, `GET /kitchens/{id}/restaurants`, esta última com 404 se a cozinha não
  existir).
- [ ] **4.7** Testes: de persistência (ida e volta do restaurante **fora de transação**, para provar o `LAZY`; FK e
  índice lidos do schema real) e de API.
- [ ] **4.8** Lado de escrita do restaurante: `RestaurantCommandService` (criar restaurante validando a cozinha pelo
  `KitchenLookup`, renomear, trocar de cozinha) e os endpoints correspondentes. Teste: excluir uma cozinha que tem
  restaurante devolve 409 e a cozinha continua existindo (depende do item 3.7.1).

---

## 5. Módulo `payment`

Terceiro módulo. Segue o roteiro da seção 3, de dentro para fora. Os campos do `Payment` são os que a aula definir;
ele ainda não se liga a `restaurant` (item 7.4).

- [x] **5.1** Decisão tomada: os enums do `Payment` (forma de pagamento, status) ficam em `payment/domain` (por
  exemplo `PaymentMethod`, ao lado do `Payment`) e a `presentation` **não os conhece**: Request e Response levam
  `String`, e a `application` converte (`PaymentMethod.from(String)` na entrada e `name()` na saída). Um valor
  desconhecido vira `DomainException` (422). Os motivos, um exemplo e os custos estão em `architecture.md`, seções 10
  e 11. Por isso a `presentation` não tem exceção para enums (só o `DomainException`).
- [ ] **5.2** `payment/domain`: `PaymentId`, os value objects dos campos, os enums (por exemplo `PaymentMethod`, com a
  fábrica `from(String)` em `switch` explícito), `Payment` (`create`, `restore`) e `PaymentRepository`. Testes de
  unidade do domínio, incluindo a fábrica (valor válido e valor desconhecido → `DomainException`).
- [ ] **5.3** `payment/infrastructure/persistence`: `PaymentEntity` (`tb_payment`), `PaymentMapper`,
  `SpringDataPaymentRepository` e `JpaPaymentRepositoryAdapter`.
- [ ] **5.4** `payment/application`: `PaymentView`, `CreatePaymentCommand` (os dois com `String` nos campos de enum),
  `PaymentNotFoundException`, `PaymentQueryRepository`, `PaymentCommandService` (converte a `String` com a fábrica do
  enum) e `PaymentQueryService`. Testes dos services com Mockito, incluindo o valor de enum desconhecido.
- [ ] **5.5** Leitura na persistência (`SpringDataPaymentQueryRepository` e `JpaPaymentQueryRepositoryAdapter`) com
  teste `@DataJpaTest`. A consulta devolve o enum como texto: `cast(p.method as string)` dentro do `select new`, ou um
  construtor extra no `PaymentView` que receba o enum.
- [ ] **5.6** `payment/presentation`: DTOs (campos de enum como `String`), `PaymentWebMapper` e `PaymentController`
  (`POST /payments`, `GET /payments/{id}`, `GET /payments`). Testes de API, incluindo o valor de enum desconhecido
  (422) e os valores do JSON de cada enum.
- [ ] **5.7** Rodar `./mvnw clean test` e abrir o PR do módulo (`feat/payment-module`).

---

## 6. Qualidade e acabamento

- [ ] **6.1** 🟠 `README.md` na raiz: o que é o projeto, como rodar e link para `docs/architecture.md`.
- [ ] **6.2** 🟢 Teste de fumaça (`@SpringBootTest` com `MockMvc`) percorrendo o ciclo de vida de cada módulo pelo HTTP.
- [ ] **6.3** 🟢 Documentação da API (springdoc-openapi). Como a API usa `String` nos enums, os valores permitidos não
  aparecem sozinhos: listá-los com `@Schema(allowableValues = ...)`.
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
  segue o roteiro da seção 4 (`api`, port, `acl`). Se o `restaurant` precisar do enum `PaymentMethod`, é o momento de
  movê-lo para `shared/domain` (ou de dar ao `restaurant` o seu próprio enum, traduzido pelo `acl`).
- [ ] **7.5** Demais entidades e módulos: seguir o roteiro da seção 3, respeitando as regras de referência
  entre agregados (id no domínio, associação direta na persistência).
- [ ] **7.6** Validações personalizadas e demais tópicos conforme surgirem.
- [ ] **7.7** Teste de arquitetura com ArchUnit (dependência `archunit-junit5`, escopo `test`), para travar sozinho as
  regras da seção 10 do `architecture.md`: `domain` sem Spring, JPA ou JSON; `presentation` só fala com `application`
  (exceção: o `GlobalExceptionHandler` enxerga o `DomainException`); `application` sem `infrastructure`,
  `presentation` e JPA e, do Spring, só `stereotype` e `transaction` (liberar `org.springframework.data.domain` quando
  entrar a paginação, item 7.1); módulos sem ciclos; só `infrastructure.acl` usa a `api` do vizinho; entidades JPA do
  vizinho só em `infrastructure.persistence`. Cada regra com `.because("...")`. Até lá, as regras valem por convenção e
  revisão. Vale adicionar quando já houver dois módulos, e provar cada regra provocando uma violação de propósito.
