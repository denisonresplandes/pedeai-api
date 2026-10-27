package dev.denison.api.pedeai.presentation.restaurant;

import dev.denison.api.pedeai.core.application.restaurant.RestaurantCommandHandler;
import dev.denison.api.pedeai.core.application.restaurant.RestaurantQueryHandler;
import dev.denison.api.pedeai.core.application.restaurant.dto.RestaurantResponse;
import dev.denison.api.pedeai.presentation.restaurant.dto.CreateRestaurantRequest;
import dev.denison.api.pedeai.presentation.restaurant.dto.UpdateRestaurantRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/restaurants")
@Validated
public class RestaurantController {

    private final RestaurantQueryHandler queryHandler;
    private final RestaurantCommandHandler commandHandler;

    public RestaurantController(RestaurantQueryHandler queryHandler,
                                RestaurantCommandHandler commandHandler) {
        this.queryHandler = queryHandler;
        this.commandHandler = commandHandler;
    }

    @GetMapping
    public List<RestaurantResponse> find(@RequestParam(value = "name", required = false) String name) {
        if (Objects.nonNull(name)) {
            return queryHandler.findByName(name);
        }
        return queryHandler.findAll();
    }

    @GetMapping("/{id}")
    public RestaurantResponse findById(@PathVariable
                                    @Positive(message = "{restaurant.id.positive}")
                                    Long id) {
        return queryHandler.findById(id);
    }

    @PutMapping("/{id}")
    public RestaurantResponse update(@PathVariable
                                     @Positive(message = "{restaurant.id.positive}")
                                     Long id,
                                     @RequestBody @Valid UpdateRestaurantRequest request) {
        return commandHandler.update(request.toCommand(id));
    }

    @PostMapping
    public ResponseEntity<RestaurantResponse> create(@RequestBody @Valid CreateRestaurantRequest request,
                                                     UriComponentsBuilder uriBuilder) {
        RestaurantResponse response = commandHandler.create(request.toCommand());
        URI uri = uriBuilder.path("/restaurants/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable
                                       @Positive(message = "{restaurant.id.positive}")
                                       Long id) {
        commandHandler.delete(id);
        return ResponseEntity.noContent().build();
    }
}
