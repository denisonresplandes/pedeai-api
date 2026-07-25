package dev.denison.api.pedeai.presentation.kitchen;

import dev.denison.api.pedeai.application.kitchen.KitchenCommandHandler;
import dev.denison.api.pedeai.application.kitchen.KitchenQueryHandler;
import dev.denison.api.pedeai.application.kitchen.dto.DeleteKitchenCommand;
import dev.denison.api.pedeai.application.kitchen.dto.KitchenResponse;
import dev.denison.api.pedeai.presentation.kitchen.dto.CreateKitchenRequest;
import dev.denison.api.pedeai.presentation.kitchen.dto.UpdateKitchenRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Objects;

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
    // TODO mover mensagens de validação para arquivo.

    @GetMapping
    public ResponseEntity<?> find(@RequestParam(value = "name", required = false) String name) {
        if (Objects.nonNull(name)) {
            return ResponseEntity.ok(queryHandler.findByName(name));
        }
        return ResponseEntity.ok(queryHandler.findAll());
    }

    @GetMapping("/{id}")
    public KitchenResponse findById(@PathVariable
                                    @NotNull(message = "The kitchen id cannot be null")
                                    @Positive(message = "The kitchen id must be a positive number")
                                    Long id) {
        return queryHandler.findById(id);
    }

    /**
     * Deixa a transação aberta (até o fim do método_) para que o Hibernate
     * mantenha o cache de primeiro nível ativo e execute o UPDATE sem SELECTs extras.
     */
    @Transactional
    @PutMapping("/{id}")
    public KitchenResponse update(@PathVariable
                                  @NotNull(message = "The kitchen id cannot be null")
                                  @Positive(message = "The kitchen id must be a positive number")
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

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable
                                       @NotNull(message = "The kitchen id cannot be null")
                                       @Positive(message = "The kitchen id must be a positive number")
                                       Long id) {
        commandHandler.delete(new DeleteKitchenCommand(id));
        return ResponseEntity.noContent().build();
    }
}
