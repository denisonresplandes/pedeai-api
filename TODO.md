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
| B6 | Sem handler para `DataIntegrityViolationException` | Corrida entre `existsByName` e o INSERT, ou entre "kitchen em uso" e um INSERT de restaurant → 500 em vez de 409 | 5 |
| B7 | `Kitchen` não recebeu a correção case-insensitive do `Restaurant` (`existsByName` é case-sensitive) | Só funciona por causa da collation `_ci` do MySQL; em outro banco "indiana" passaria e daria 500 no unique | 6 |
| B8 | `paymentMethods` aceita `null` dentro do Set | `{"paymentMethods": ["CASH", null]}` → erro de banco → 500 | 7 |
| B9 | Mensagem de `Money` diz "cannot be negative", mas a regra também rejeita zero | Mensagem enganosa | 8 |
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
- [ ] **5. (B6)** Handler de `DataIntegrityViolationException` → 409 no `GlobalExceptionHandler`.
- [ ] **6. (B7)** `KitchenRepository.existsByName` case-insensitive (`existsByNameIgnoreCase`), igual ao Restaurant.
- [ ] **7. (B8)** `Restaurant`: validar `null` em `addPaymentMethod`/`removePaymentMethod`; `create`/`restore` passam a receber `paymentMethods` (CASH obrigatório só no `create`; hoje o construtor força `CASH` até ao restaurar do banco). Mapper e handler deixam de fazer `forEach(addPaymentMethod)`.
- [ ] **8. (B9)** Corrigir a mensagem (ou a regra) de `Money`.

### Fase 2 — Redundância (DTOs / Records)

Fluxo de escrita de uma cozinha hoje: `Request → Command → KitchenName → Kitchen → KitchenEntity → Kitchen → Response` (7 tipos). Leitura: `KitchenEntity → KitchenSummary → KitchenResponse`. Manter `Request`, `Command`, `Response`, VOs, `Entity` e `Mapper` (cada um tem uma razão); reduzir o resto.

- [ ] **9. Validação em duas camadas, não três.** Ficar com Request (mensagem amigável por campo) + VO (invariante). `Create/Update*Command` viram records sem validação; `ValidationException` perde os helpers duplicados de `DomainException` (sobra a checagem de command nulo). Remover `@NotNull` de `@PathVariable` (nunca é nula) e as chaves `*.id.required` que ficarem sem uso.
- [ ] **10.** Mover `isEquals` (duplicado nos dois handlers) para o VO (`equalsIgnoreCase`), com `Locale.ROOT`.
- [ ] **11.** `GlobalExceptionHandler`: extrair helper `problem(status, title, detail)` no lugar dos 6 métodos quase iguais. Aproveitar para tratar `HttpMessageNotReadableException` (JSON malformado, enum inexistente como `"status":"FOO"`), que hoje devolve 400 no formato padrão do Spring em vez de `ProblemDetail`.
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
