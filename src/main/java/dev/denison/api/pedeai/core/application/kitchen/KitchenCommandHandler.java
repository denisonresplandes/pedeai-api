package dev.denison.api.pedeai.core.application.kitchen;

import dev.denison.api.pedeai.core.application.kitchen.dto.CreateKitchenCommand;
import dev.denison.api.pedeai.core.application.kitchen.dto.KitchenResponse;
import dev.denison.api.pedeai.core.application.kitchen.dto.UpdateKitchenCommand;
import dev.denison.api.pedeai.core.application.kitchen.exceptions.KitchenInUseException;
import dev.denison.api.pedeai.core.application.kitchen.exceptions.KitchenNameAlreadyExistsException;
import dev.denison.api.pedeai.core.application.kitchen.exceptions.KitchenNotFoundException;
import dev.denison.api.pedeai.core.application.restaurant.RestaurantQueryRepository;
import dev.denison.api.pedeai.core.application.shared.ValidationException;
import dev.denison.api.pedeai.core.domain.kitchen.Kitchen;
import dev.denison.api.pedeai.core.domain.kitchen.KitchenRepository;
import dev.denison.api.pedeai.core.domain.kitchen.vo.KitchenId;
import dev.denison.api.pedeai.core.domain.kitchen.vo.KitchenName;

public class KitchenCommandHandler {

    private final KitchenRepository repository;
    private final RestaurantQueryRepository restaurantQueryRepository;

    public KitchenCommandHandler(KitchenRepository repository,
                                 RestaurantQueryRepository restaurantQueryRepository) {
        this.repository = repository;
        this.restaurantQueryRepository = restaurantQueryRepository;
    }

    public KitchenResponse create(CreateKitchenCommand command) {
        validate(command, "The create kitchen command cannot be null.");
        KitchenName name = new KitchenName(command.name());
        ensureNameIsUnique(name);
        Kitchen kitchenToSave = Kitchen.create(name);
        Kitchen kitchenSaved = repository.save(kitchenToSave);
        return KitchenResponseMapper.toResponse(kitchenSaved);
    }

    public KitchenResponse update(UpdateKitchenCommand command) {
        validate(command, "The update kitchen command cannot be null.");
        KitchenName newName = new KitchenName(command.name());
        Kitchen kitchen = getKitchenOrElseThrow(new KitchenId(command.id()));

        if (!kitchen.getName().equalsIgnoreCase(newName)) {
            ensureNameIsUnique(newName);
        }
        /*
        * Se o nome enviado for igual ao atual,
        * o Hibernate não executa o UPDATE no banco.
        * */
        kitchen.changeName(newName);
        Kitchen kitchenUpdated = repository.save(kitchen);
        return KitchenResponseMapper.toResponse(kitchenUpdated);
    }

    /*
     * Recebe o id direto, sem Command: uma exclusão só precisa do id e não carrega dados, então um
     * DeleteKitchenCommand seria um Long embrulhado (as consultas também recebem o Long). O id é
     * validado pelo VO KitchenId. Se a exclusão um dia ganhar dados (ex.: motivo), o Command volta.
     */
    public void delete(Long id) {
        Kitchen kitchenToDelete = getKitchenOrElseThrow(new KitchenId(id));
        if (restaurantQueryRepository.existsByKitchenId(kitchenToDelete.getId())) {
            throw new KitchenInUseException(kitchenToDelete.getId().value());
        }
        repository.delete(kitchenToDelete);
    }

    private void validate(Object o, String message) {
        ValidationException.requireNonNull(o, message);
    }

    private void ensureNameIsUnique(KitchenName name) {
        /*
         * checa se já existe alguma outra Kitchen
         * no banco com este nome.
         * */
        if (repository.existsByName(name)) {
            throw new KitchenNameAlreadyExistsException(name.value());
        }
    }

    private Kitchen getKitchenOrElseThrow(KitchenId id) {
        return repository.findById(id).orElseThrow(() ->
                new KitchenNotFoundException(id.value())
        );
    }
}