package dev.denison.api.pedeai.core.application.restaurant;

import dev.denison.api.pedeai.core.application.kitchen.KitchenQueryRepository;
import dev.denison.api.pedeai.core.application.kitchen.exceptions.KitchenNotFoundException;
import dev.denison.api.pedeai.core.application.restaurant.dto.CreateRestaurantCommand;
import dev.denison.api.pedeai.core.application.restaurant.dto.DeleteRestaurantCommand;
import dev.denison.api.pedeai.core.application.restaurant.dto.RestaurantResponse;
import dev.denison.api.pedeai.core.application.restaurant.dto.UpdateRestaurantCommand;
import dev.denison.api.pedeai.core.application.restaurant.exceptions.RestaurantNameAlreadyExistsException;
import dev.denison.api.pedeai.core.application.restaurant.exceptions.RestaurantNotFoundException;
import dev.denison.api.pedeai.core.application.shared.ValidationException;
import dev.denison.api.pedeai.core.domain.kitchen.vo.KitchenId;
import dev.denison.api.pedeai.core.domain.restaurant.Restaurant;
import dev.denison.api.pedeai.core.domain.restaurant.RestaurantRepository;
import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantId;
import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantName;

public class RestaurantCommandHandler {

    private final RestaurantRepository repository;
    private final KitchenQueryRepository kitchenQueryRepository;

    public RestaurantCommandHandler(RestaurantRepository repository,
                                    KitchenQueryRepository kitchenQueryRepository) {
        this.repository = repository;
        this.kitchenQueryRepository = kitchenQueryRepository;
    }

    public RestaurantResponse create(CreateRestaurantCommand command) {
        validate(command, "The create restaurant command cannot be null.");

        // Primeiro monta todos os VOs (só valida, não acessa o banco); depois consulta. Assim uma
        // entrada inválida é recusada sem nenhuma ida ao banco, como acontecia com o Command.
        RestaurantName name = new RestaurantName(command.name());
        KitchenId kitchenId = new KitchenId(command.kitchenId());

        ensureNameIsUnique(name);
        ensureKitchenWithIdExists(kitchenId);

        Restaurant restaurantToSave = Restaurant.create(name, kitchenId, command.paymentMethods());
        Restaurant restaurantSaved = repository.save(restaurantToSave);
        return RestaurantResponseMapper.toResponse(restaurantSaved);
    }

    public RestaurantResponse update(UpdateRestaurantCommand command) {
        validate(command, "The update restaurant command cannot be null.");

        RestaurantName newName = new RestaurantName(command.name());
        Restaurant restaurant = getRestaurantOrElseThrow(new RestaurantId(command.id()));

        if (!restaurant.getName().equalsIgnoreCase(newName)) {
            ensureNameIsUnique(newName);
        }
        restaurant.changeName(newName);
        Restaurant restaurantUpdated = repository.save(restaurant);
        return RestaurantResponseMapper.toResponse(restaurantUpdated);
    }

    public void delete(DeleteRestaurantCommand command) {
        validate(command, "The delete restaurant command cannot be null.");
        RestaurantId id = new RestaurantId(command.id());
        if (!repository.existsById(id)) {
            throw new RestaurantNotFoundException(id.value());
        }
        repository.deleteById(id);
    }

    private void validate(Object o, String message) {
        ValidationException.requireNonNull(o, message);
    }

    private void ensureNameIsUnique(RestaurantName name) {
        if (repository.existsByName(name)) {
            throw new RestaurantNameAlreadyExistsException(name.value());
        }
    }

    private Restaurant getRestaurantOrElseThrow(RestaurantId id) {
        return repository.findById(id).orElseThrow(() ->
                new RestaurantNotFoundException(id.value())
        );
    }

    private void ensureKitchenWithIdExists(KitchenId kitchenId) {
        if (!kitchenQueryRepository.existsById(kitchenId)) {
            throw new KitchenNotFoundException(kitchenId.value());
        }
    }
}
