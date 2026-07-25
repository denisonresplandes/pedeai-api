package dev.denison.api.pedeai.application.kitchen;

import dev.denison.api.pedeai.application.kitchen.dto.CreateKitchenCommand;
import dev.denison.api.pedeai.application.kitchen.dto.DeleteKitchenCommand;
import dev.denison.api.pedeai.application.kitchen.dto.KitchenResponse;
import dev.denison.api.pedeai.application.kitchen.dto.UpdateKitchenCommand;
import dev.denison.api.pedeai.application.kitchen.exceptions.KitchenNameAlreadyExistsException;
import dev.denison.api.pedeai.application.kitchen.exceptions.KitchenNotFoundException;
import dev.denison.api.pedeai.application.shared.ValidationException;
import dev.denison.api.pedeai.domain.kitchen.Kitchen;
import dev.denison.api.pedeai.domain.kitchen.KitchenRepository;
import dev.denison.api.pedeai.domain.kitchen.vo.KitchenId;
import dev.denison.api.pedeai.domain.kitchen.vo.KitchenName;

public class KitchenCommandHandler {

    private final KitchenRepository repository;

    public KitchenCommandHandler(KitchenRepository repository) {
        this.repository = repository;
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

        if (!kitchen.getName().equals(newName)) {
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

    public void delete(DeleteKitchenCommand command) {
        validate(command, "The delete kitchen command cannot be null.");
        Kitchen kitchenToDelete = getKitchenOrElseThrow(new KitchenId(command.id()));
        repository.delete(kitchenToDelete);
    }

    private void validate(Object o, String message) {
        ValidationException.requireNonNull(o, message);
    }

    private void ensureNameIsUnique(KitchenName name) {
        if (repository.existsByName(name)) {
            throw new KitchenNameAlreadyExistsException("This %s already exists."
                    .formatted(name.value())
            );
        }
    }

    private Kitchen getKitchenOrElseThrow(KitchenId id) {
        return repository.findById(id).orElseThrow(() ->
                new KitchenNotFoundException(id.value())
        );
    }
}