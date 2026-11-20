package dev.denison.pedeai.kitchen;

import dev.denison.pedeai.kitchen.dto.CreateKitchenRequest;
import dev.denison.pedeai.kitchen.dto.KitchenResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/kitchens")
public class KitchenController {

    private final KitchenService service;

    public KitchenController(KitchenService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<KitchenResponse> create(@RequestBody @Valid CreateKitchenRequest request,
                                                  UriComponentsBuilder uriBuilder) {
        Kitchen kitchenSaved = service.create(request.name());
        URI location = uriBuilder.path("/kitchens/{id}")
                .buildAndExpand(kitchenSaved.getId())
                .toUri();
        KitchenResponse response = KitchenMapper.toResponse(kitchenSaved);
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public KitchenResponse findById(@PathVariable Long id) {
        return KitchenMapper.toResponse(service.findById(id));
    }

    @GetMapping
    public List<KitchenResponse> findAll() {
        return service.findAll()
                .stream()
                .map(KitchenMapper::toResponse)
                .toList();
    }
}
