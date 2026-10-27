package dev.denison.api.pedeai.core.application.kitchen;

import dev.denison.api.pedeai.core.application.kitchen.dto.CreateKitchenCommand;
import dev.denison.api.pedeai.core.application.kitchen.dto.KitchenResponse;
import dev.denison.api.pedeai.core.application.kitchen.dto.UpdateKitchenCommand;

/*
 * Contrato dos casos de uso de ESCRITA de Kitchen: é o que o controller conhece.
 *
 * Por que uma interface, se só havia uma classe? Para criar uma "costura" entre o controller e as
 * regras de negócio. Com ela, dá para colocar um objeto na frente do handler (um decorator) sem que
 * o controller perceba:
 *
 *     KitchenController -> TransactionalKitchenCommandHandlerDecorator -> DefaultKitchenCommandHandler
 *                          (infraestrutura: abre/fecha a transação)       (core: só regras)
 *
 * Assim o core continua Java puro, sem @Transactional, e a transação fica num lugar só, por caso de uso.
 * Só o lado do comando precisa disso; as consultas (KitchenQueryHandler) não usam transação.
 */
public interface KitchenCommandHandler {

    KitchenResponse create(CreateKitchenCommand command);

    KitchenResponse update(UpdateKitchenCommand command);

    void delete(Long id);
}
