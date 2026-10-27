package dev.denison.api.pedeai.presentation.kitchen;

import dev.denison.api.pedeai.core.application.kitchen.KitchenCommandHandler;
import dev.denison.api.pedeai.core.application.kitchen.KitchenQueryHandler;
import dev.denison.api.pedeai.core.application.kitchen.dto.KitchenResponse;
import dev.denison.api.pedeai.presentation.kitchen.dto.CreateKitchenRequest;
import dev.denison.api.pedeai.presentation.kitchen.dto.UpdateKitchenRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/*
* O controller poderia ser dividido em Command e Query.
* */

@RestController
@RequestMapping("/kitchens")
@Validated
public class KitchenController {

    private final KitchenQueryHandler queryHandler;
    private final KitchenCommandHandler commandHandler;

    public KitchenController(KitchenQueryHandler queryHandler,
                             KitchenCommandHandler commandHandler) {
        this.queryHandler = queryHandler;
        this.commandHandler = commandHandler;
    }

    @GetMapping
    public List<KitchenResponse> find(@RequestParam(value = "name", required = false) String name) {
        if (Objects.nonNull(name)) {
            return queryHandler.findByName(name);
        }
        return queryHandler.findAll();
    }

    @GetMapping("/{id}")
    public KitchenResponse findById(@PathVariable
                                    @Positive(message = "{kitchen.id.positive}")
                                    Long id) {
        return queryHandler.findById(id);
    }

    /*
     * A transação (que mantém a cache de 1º nível e evita SELECTs extras) não é mais daqui: ela é aberta
     * pelo TransactionalKitchenCommandHandlerDecorator, em volta do caso de uso.
     */
    @PutMapping("/{id}")
    public KitchenResponse update(@PathVariable
                                  @Positive(message = "{kitchen.id.positive}")
                                  Long id,
                                  @RequestBody @Valid UpdateKitchenRequest request) {
        return commandHandler.update(request.toCommand(id));
    }

    @PostMapping
    public ResponseEntity<KitchenResponse> create(@RequestBody @Valid CreateKitchenRequest request,
                                                  UriComponentsBuilder uriBuilder) {
        KitchenResponse response = commandHandler.create(request.toCommand());
        URI uri = uriBuilder.path("/kitchens/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable
                                       @Positive(message = "{kitchen.id.positive}")
                                       Long id) {
        commandHandler.delete(id);
        return ResponseEntity.noContent().build();
    }
}
