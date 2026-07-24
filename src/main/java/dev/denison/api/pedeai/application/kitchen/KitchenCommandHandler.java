package dev.denison.api.pedeai.application.kitchen;

import dev.denison.api.pedeai.application.kitchen.dto.CreateKitchenCommand;
import dev.denison.api.pedeai.application.kitchen.dto.DeleteKitchenCommand;
import dev.denison.api.pedeai.application.kitchen.dto.KitchenResponse;
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

    public void delete(DeleteKitchenCommand command) {
        validate(command, "The delete kitchen command cannot be null.");
        KitchenId kitchenId = new KitchenId(command.id());
        checkExistence(kitchenId);
        repository.deleteById(kitchenId);
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

    private void checkExistence(KitchenId id) {
        if (!repository.existsById(id)) {
            throw new KitchenNotFoundException(id.value());
        }
    }
}