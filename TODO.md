# TODO — PedeAí

Este arquivo é atualizado conforme o curso avança. As etapas estão na ordem em que devem ser feitas (e commitadas).

## Arquitetura utilizada

Clean Architecture + CQRS leve + DDD leve (domínio rico). É uma escolha deliberada: o curso usa arquitetura em camadas tradicional.

```
presentation  →  core.application  →  core.domain
infrastructure →  core.application  →  core.domain      (setas = "depende de")
```

| Camada | Pacote | O que tem | Regra |
|---|---|---|---|
| Domínio | `core.domain` | Aggregates (`Kitchen`, `Restaurant`, `Payment`), VOs (`KitchenId`, `KitchenName`, `Money`...), portas de escrita (`*Repository`), `DomainException` | Java puro: sem Spring, JPA ou Jakarta. Guarda as invariantes. |
| Aplicação | `core.application` | `*CommandHandler` (escrita), `*QueryHandler` (leitura), `*Command`, `*Response`, exceções de aplicação, portas de saída (`PaymentProcessorStrategy`) | Java puro: sem anotações do Spring (nem `@Service`/`@Transactional`). Handlers são montados em `infrastructure/configs/*Config`. |
| Infraestrutura | `infrastructure` | Entities JPA, mappers, adapters das portas, Spring Data, gateways (Stripe, MercadoPago, Cash), configs | Implementa as portas do core. |
| Apresentação | `presentation` | Controllers, `*Request` (Bean Validation), `GlobalExceptionHandler` | Traduz HTTP ⇄ Command/Response. |

Padrões em uso:
- **CQRS leve:** `*CommandHandler` + `*Repository` para escrita; `*QueryHandler` + `*QueryRepository` para leitura (devolve projeção, não o aggregate).
- **Aggregate:** construtor privado, `create(...)` (sem id) e `restore(...)` (com id, usado só pelo mapper da infra). `equals` por id, `hashCode` constante.
- **Referência entre aggregates por ID** (`Restaurant` guarda `KitchenId`, não `Kitchen`).
- **Ports & Adapters:** `XRepositoryAdapter` converte `Entity ⇄ domínio` com `XMapper` e delega ao Spring Data.
- **Erros:** `DomainException` → 400, `ValidationException` → 400, `ResourceNotFoundException` → 404, `ConflictException` → 409, todos como `ProblemDetail`.

Regra de ouro: **todas as classes em `core.*` (domain e application) são Java puro, sem anotações do Spring** (nem Jakarta/Hibernate). `core.domain` nunca importa `application`, `infrastructure` ou `presentation`; `core.application` nunca importa `infrastructure` ou `presentation`. Verificado em 2026-10-23: nenhum import de framework em `core`.

---

## Bugs conhecidos

| # | Bug | Como reproduzir / efeito | Etapa |
|---|---|---|---|
| B1 | Chave de mensagem com typo em `CreateRestaurantRequest.java:17` (`{kitchen.id.require}`; a existente é `kitchen.id.required`) | `POST /restaurants` sem `kitchenId` devolve o texto literal `{kitchen.id.require}` | 1 ✅ |
| B2 | Nome com mais de 50 caracteres não é validado (coluna `length = 50`) | `POST /kitchens` com nome de 51+ caracteres → 500 (erro de banco) | 2 ✅ |
| B3 | Regra "nome ≥ 2 caracteres" não existe no domínio (`KitchenName`, `RestaurantName`); só em Request, Command e Summary | Quem criar o VO direto (teste, outro caso de uso) burla a regra | 2 ✅ |
| B4 | Webhook do Stripe sem `@Valid` | `POST /webhooks/stripe` sem `status` → NPE → 500; `@NotNull`/`@Positive` do payload nunca rodam | 3 ✅ |
| B5 | `PaymentNotFoundException` e `UnsupportedPaymentMethodException` estendem `RuntimeException` | A segunda não tem handler → 500 | 4 ✅ |
| B6 | Sem handler para `DataIntegrityViolationException` | Corrida entre `existsByName` e o INSERT, ou entre "kitchen em uso" e um INSERT de restaurant → 500 em vez de 409 | 5 ✅ |
| B7 | `Kitchen` não recebeu a correção case-insensitive do `Restaurant` (`existsByName` é case-sensitive) | Só funciona por causa da collation `_ci` do MySQL; num banco case-sensitive (PostgreSQL, H2) "indiana" e "Indiana" coexistiriam, quebrando a regra que o Restaurant já garante | 6 ✅ |
| B8 | `Restaurant` não protegia `paymentMethods`: aceitava `null` em `addPaymentMethod`/`removePaymentMethod` e dentro do `Set` recebido, e `restore` forçava `CASH` até ao reconstruir do banco | Pela API o `null` não chegava ao domínio (o Jackson 3 já recusa `null` em array: 400 fora do formato `ProblemDetail`, ver etapa 11); qualquer outro chamador do domínio o levaria até o banco (`payment_method` é NOT NULL) | 7 ✅ |
| B9 | Mensagem de `Money` diz "cannot be negative", mas a regra também rejeita zero | Mensagem enganosa (só visível a quem usa o domínio direto: pela API o `@Positive` do request barra zero e negativo antes) | 8 ✅ |
| B10 | `POST /payments` mantém transação aberta durante a chamada ao gateway e salva antes de saber se o método é suportado | Chamada externa lenta segura conexão de banco; rollback usado como controle de fluxo | 17 |
| B11 | Webhook não é idempotente | Stripe reenvia o evento; `markAsPaid` em pagamento `PAID` lança `DomainException` → 400 → Stripe reenvia de novo | 21 |

---

## Etapas

### Etapa 0 — Já feito
- [x] GET de restaurant sem SELECT extra da kitchen (`JOIN FETCH r.kitchen`; `fetch = EAGER` é ignorado em `@Query` JPQL).
- [x] `delete` de restaurant: `existsById` + `deleteById` (antes carregava duas vezes).
- [x] Unicidade do nome de restaurant case-insensitive (`existsByNameIgnoreCase`).
- [x] `import.sql` com comentários `--`.
- [x] Apagar kitchen com restaurantes vinculados → `KitchenInUseException` (409) em vez de erro de FK (500).
- [x] `update` de restaurant por dirty checking (o ganho real depende da etapa 16).
- [x] Revisão geral da arquitetura (esta lista).

### Fase 1 — Correção de bugs
- [x] **1. (B1)** Corrigir a chave `{kitchen.id.require}` → `{kitchen.id.required}`. Criado `RestaurantControllerTest` (`@WebMvcTest`) com 2 casos: `kitchenId` ausente e `kitchenId` não positivo.
- [x] **2. (B2, B3)** Regras de nome nos VOs `KitchenName`/`RestaurantName` (mínimo 2, máximo 50, medidos após o trim), expostas como `MIN_LENGTH`/`MAX_LENGTH`; `@Size(min, max)` dos 4 Requests e `length` da coluna nas Entities leem essas constantes (fonte única); mensagens `*.name.size` com `{min}`/`{max}`. Testes: `KitchenNameTest`, `RestaurantNameTest`, novo `KitchenControllerTest` e casos extras no `RestaurantControllerTest`. Validado no app real (51 caracteres: 500 → 400).
- [x] **3. (B4)** `@Valid` no `StripeWebhookController`; `StripeWebhookPayload` ganhou `@NotNull` no `paymentId` (`@Positive` sozinho aceita `null`) e mensagens `payment.id.*`/`payment.status.required`. Teste novo: `StripeWebhookControllerTest` (5 casos, verifica também que o handler do core não é chamado com payload inválido). Validado no app real (sem `status`: 500 → 400).
- [x] **4. (B5)** `PaymentNotFoundException extends ResourceNotFoundException` (handler dedicado removido do `GlobalExceptionHandler`; o `title` da resposta passa de "Payment Not Found" para "Resource Not Found", igual a Kitchen/Restaurant) e `UnsupportedPaymentMethodException extends ValidationException` (400). Testes novos: `PaymentCommandHandlerTest` (hierarquia), `PaymentControllerTest` e um caso de 404 no `StripeWebhookControllerTest`. O 404 foi validado no app real; o caso "método não suportado" não é alcançável via HTTP hoje (todo valor do enum tem um processor), então ficou coberto só pelos testes.
- [x] **5. (B6)** Handler de `DataIntegrityViolationException` → 409 no `GlobalExceptionHandler`, com mensagem genérica (o SQL vai só para o log, não para o cliente). Testes: 2 casos no `KitchenControllerTest` (unique no create, FK no delete). Validado no app real com 30 `POST /kitchens` simultâneos de mesmo nome: antes até 29 respostas 500 numa rodada, depois 1×201 + 29×409 e nenhum 500 no log. Limitação documentada no código: o handler também cobre NOT NULL e dado grande demais, que hoje são barrados antes pela validação.
- [x] **6. (B7)** `SpringDataKitchenRepository.existsByName` → `existsByNameIgnoreCase` (o `KitchenRepositoryAdapter` passa a chamá-lo), igual ao Restaurant. O MySQL (`_ci`) esconde o bug, então ele não é reproduzível no app real nem coberto por teste automatizado: provei com um `@DataJpaTest` em H2 (case-sensitive), que falhou antes da correção e passou depois, e depois removi o H2 e o teste para não manter um banco só para isso. Validado no app real: o comportamento HTTP não muda (continua 409), mas o SQL passou de `where name=?` para `where upper(name)=upper(?)`.
- [x] **7. (B8)** `Restaurant`: `addPaymentMethod`/`removePaymentMethod` rejeitam `null` (`DomainException`); `create`/`restore` passam a receber `paymentMethods` (cópia defensiva, `null` no conjunto ou dentro dele rejeitado). `CASH` é acrescentado só no `create`; o `restore` mantém exatamente o que veio do banco (antes o construtor forçava `CASH` até ao restaurar). `RestaurantMapper.toDomain` e `RestaurantCommandHandler.create` deixam de fazer `forEach(addPaymentMethod)`. Testes: novo `RestaurantTest` (14 casos, domínio puro) e 2 casos novos no `RestaurantCommandHandlerTest`. Validado no app real: criação com/sem métodos continua devolvendo `CASH` sem duplicar, e o `PUT` não emite nenhum `INSERT`/`DELETE` em `tb_restaurant_payment_method`. O `null` no array não era reproduzível pela API (o Jackson 3 o barra antes, com 400 fora do `ProblemDetail`); a reprodução foi por teste unitário, que falhou antes da correção.
- [x] **8. (B9)** Mensagem de `Money` corrigida para `amount must be greater than zero` (a regra `<= 0` está certa: pagamento de valor zero não faz sentido, e o `@Positive` do request a espelha; o que estava errado era o texto). Teste novo: `MoneyTest` (5 casos, domínio puro), que falhou antes da correção com `expected: <amount must be greater than zero> but was: <amount cannot be negative>`. Validado no app real: `POST /payments` com `0` e `-5` dá 400 pelo `@Positive` (a mensagem do `Money` nem é alcançada pela API) e `0.01`/`10.50` dão 202.

### Fase 2 — Redundância (DTOs / Records)

Fluxo de escrita de uma cozinha hoje: `Request → Command → KitchenName → Kitchen → KitchenEntity → Kitchen → Response` (7 tipos). Leitura: `KitchenEntity → KitchenSummary → KitchenResponse`. Manter `Request`, `Command`, `Response`, VOs, `Entity` e `Mapper` (cada um tem uma razão); reduzir o resto.

- [x] **9. Validação em duas camadas, não três.** Ficou Request (mensagem amigável por campo, na borda) + VO/domínio (invariante). Todos os Commands viraram records sem validação (também `Delete*`, `ConfirmPaymentCommand` e `ProcessPaymentCommand`, porque usavam os helpers removidos; o domínio já cobre: `KitchenId`/`RestaurantId`/`PaymentId` exigem positivo, `Payment` exige método e valor). `ValidationException` perdeu `requireNonBlank`/`requirePositive` e ficou só com `requireNonNull` (command nulo). `CreateRestaurantCommand` manteve só o default `paymentMethods == null → Set.of()` (não é validação; sem ele o JSON sem `paymentMethods` viraria 400). `RestaurantCommandHandler.create` passou a montar os dois VOs antes de qualquer consulta, para recusar entrada inválida sem ir ao banco. Removidos o `@NotNull` dos 6 `@PathVariable` (nunca é nula) e a chave `restaurant.id.required`; `kitchen.id.required` e `payment.id.required` continuam em uso. Testes: 11 novos nos 3 `*CommandHandlerTest` (o VO/domínio recusa entrada inválida sem tocar nos repositórios; default de `paymentMethods` nulo). Validado no app real com uma matriz de 26 requisições comparada antes/depois: 22 idênticas; as únicas 4 que mudam são nomes como `" a "` (passam o `@Size` do Request, que não faz `trim`, mas ficam com 1 caractere), que continuam 400 e passam de `Application Validation Error` para `Domain Rule Violation`, agora com a mensagem única do VO (antes havia 4 textos diferentes para a mesma regra).
- [x] **10.** `isEquals` (duplicado nos dois handlers) virou `KitchenName.equalsIgnoreCase` / `RestaurantName.equalsIgnoreCase`; os handlers chamam o VO e perderam o método privado e o `import Objects`. Usa `String.equalsIgnoreCase`, que já é independente de locale (o `Locale.ROOT` só seria necessário com `toLowerCase`; eram alternativas, não somadas). Achado: o `isEquals` antigo usava `toLowerCase()` **sem `Locale`**, então dependia do idioma da JVM. Com `tr-TR`, `"Indiana".toLowerCase()` é `"ındiana"` e a mudança só de caixa (`Indiana` → `indiana`) virava um falso 409 ("already exists", pois a checagem de unicidade achava o próprio registro). Reproduzido no app real com `-Duser.language=tr -Duser.country=TR` (`PUT /kitchens/1` e `PUT /restaurants/1`: 409; `Ilha Verde` → `ILHA VERDE` passava, pois os dois começam com `I`). Depois da correção: 200 sob `tr-TR`, conflito real (nome de outra entidade) continua 409, e a matriz de 26 requisições no locale padrão ficou idêntica. Testes: 9 novos (2 de handler sob `tr-TR`, que falharam antes da correção; 1 de handler no locale padrão; 6 nos `*NameTest`).
- [ ] **11.** `GlobalExceptionHandler`: extrair helper `problem(status, title, detail)` no lugar dos 6 métodos quase iguais. Aproveitar para tratar `HttpMessageNotReadableException` (JSON malformado, enum inexistente como `"status":"FOO"`, `null` dentro de `paymentMethods`), que hoje devolve 400 no formato padrão do Spring em vez de `ProblemDetail`.
- [ ] **12. `Summary` ≡ `Response`.** `KitchenSummary`/`RestaurantSummary` têm os mesmos campos de `KitchenResponse`/`RestaurantResponse`. Unificar em um tipo de leitura, remover a sobrecarga `toResponse(Summary)`, tirar a validação dos tipos de leitura (dado do banco não deve virar 400 num GET) e mover as portas `*QueryRepository` de `core.domain` para `core.application` (domínio não tem consultas).
- [ ] **13.** Decidir sobre `Delete*Command` (só um `Long`; as queries recebem `Long` direto): remover ou manter por padrão.
- [ ] **14.** `GET /kitchens?name=` e `/restaurants?name=` devolvem lista (vazia se não achar), como o `GET` sem filtro; remove o `ResponseEntity<?>`.
- [ ] **15. Nits:** `StripePayloadStatus` com campo (`SUCCESS(true)`) no lugar de método abstrato por constante; `System.out.println` → logger nos processors; `Payment.getPaymentId()` → `getId()`; frase de `KitchenNameAlreadyExistsException`; `@Repository` desnecessário nas interfaces Spring Data; `unique = true` duplicado com `@UniqueConstraint`; `AggregateRoot` vazia (usar com eventos de domínio ou remover).

### Fase 3 — Arquitetura
- [ ] **16. Transação por caso de uso.** Hoje `@Transactional` está nos controllers (Kitchen update/delete, Payment, webhook), no adapter (Restaurant) e em lugar nenhum (create de Kitchen). Mover para o nível do handler **sem anotar o core** (ele é Java puro): um decorator transacional na infraestrutura (ex.: `TransactionTemplate`), aplicado ao montar os handlers nos `*Config`. Efeitos: elimina o `findById` duplicado no `update` de Restaurant, padroniza Kitchen/Restaurant e tira a transação dos controllers. Avaliar `@Version` (lost update).
- [ ] **17. (B10)** `PaymentCommandHandler.processPayment`: resolver a estratégia antes de salvar e não chamar o gateway dentro de transação de banco.
- [ ] **18. Command side só com portas de escrita.** `RestaurantCommandHandler` usa `KitchenQueryRepository.existsById` e `KitchenCommandHandler` usa `RestaurantQueryRepository.existsByKitchenId`. Mover para `KitchenRepository.existsById(KitchenId)` e `RestaurantRepository.existsByKitchenId(KitchenId)`.
- [ ] **19. Dependência circular Kitchen ⇄ Restaurant** (cada command handler conhece o outro módulo). Decidir e registrar: confiar na FK + handler 409 (etapa 5), ou domain service/evento.
- [ ] **20.** `ProcessPaymentRequest.toCommand()` não deve criar `Money` (objeto de domínio) na camada de apresentação: `ProcessPaymentCommand` carrega `BigDecimal` e o handler cria o `Money`. `RestaurantRepositoryAdapter` deixa de lançar `RestaurantNotFoundException` (exceção de aplicação) de dentro da infra.
- [ ] **ArchUnit:** teste que trava a regra de dependência (domínio sem `application`/`infrastructure`/`presentation`/Spring/Jakarta; `application` sem infra/presentation).

### Fase 4 — Domínio rico (Restaurant)
- [ ] `changeKitchen`, `addPaymentMethod` e `removePaymentMethod` só são usados no create/mapper; `UpdateRestaurantCommand` só muda o nome. Criar os casos de uso (alterar cozinha, formas de pagamento) ou aceitar que `RestaurantMapper.updateEntity` tem código inalcançável.

### Fase 5 — Pagamento (acompanha o curso)
- [ ] **21. (B11)** Webhook idempotente: `PAID` recebido de novo é no-op (200).
- [ ] `POST /payments` devolve `PaymentResponse(id, status)` + `Location` (hoje `202` sem corpo; o cliente não recebe o `paymentId` que o webhook usa).
- [ ] `PaymentProcessorStrategy.process` devolve a referência externa (QR Code PIX, client secret) quando integrar de verdade.
- [ ] `processPayment` valida `command` nulo como os outros handlers.
- [ ] Validação de assinatura do webhook e `@Version` em `PaymentEntity`.
- [ ] Ligar `Payment` a `Restaurant`/`Order` (validar que o restaurante aceita o método) e definir transições de `REFUNDED`.

### Fase 6 — Persistência
- [ ] Paginação de `findAll` de Restaurant: `JOIN FETCH` de coleção impede paginar no banco (HHH90003004); buscar IDs paginados e depois carregar as coleções (ou `@BatchSize`).
- [ ] Restaurant lê a entidade inteira e mapeia; Kitchen usa projeção. Padronizar após a etapa 12.
- [ ] Flyway no lugar de `ddl-auto=drop-and-create` + `import.sql` (IDs fixos só são seguros no MySQL).
- [ ] Credenciais do banco em `application-dev.properties` → variáveis de ambiente antes de qualquer deploy.

### Fase 7 — Testes
Cada etapa acima já traz os testes da sua mudança. Aqui ficam as lacunas de cobertura:
- [ ] Testes de controller com `@WebMvcTest` e sufixo `*Test`: `RestaurantControllerTest` e `KitchenControllerTest` já existem (validação de nome e `kitchenId`); faltam os demais endpoints (GET, DELETE, 404/409) e Payment/Stripe.
- [ ] Testes de domínio puros (sem Mockito): VOs, `Restaurant`, `Payment` (transições), `Money`.
- [ ] `@DataJpaTest`: JOIN FETCH sem N+1, unique, FK de kitchen em uso, `save` por dirty checking.
- [ ] `PedeaiApplicationTests` depende do MySQL local; usar Testcontainers/H2 quando o curso permitir.

---

## Roadmap do curso

<!-- Preencher conforme avançar: tópicos novos, o que mudou na arquitetura, decisões tomadas. -->

- [ ] (a definir)
