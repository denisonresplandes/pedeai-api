package dev.denison.api.pedeai.core.application.kitchen;

import dev.denison.api.pedeai.core.application.kitchen.dto.KitchenResponse;
import dev.denison.api.pedeai.core.application.kitchen.exceptions.KitchenNotFoundException;
import dev.denison.api.pedeai.core.domain.kitchen.KitchenQueryRepository;

import java.util.List;

public class KitchenQueryHandler {

    private final KitchenQueryRepository queryRepository;

    public KitchenQueryHandler(KitchenQueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    public KitchenResponse findById(Long id) {
        return queryRepository.findById(id)
                .map(KitchenResponseMapper::toResponse)
                .orElseThrow(() -> new KitchenNotFoundException(id));
    }
    
    public KitchenResponse findByName(String name) {
        return queryRepository.findByName(name)
                .map(KitchenResponseMapper::toResponse)
                .orElseThrow(() -> new KitchenNotFoundException(name));
    }
    
    public List<KitchenResponse> findAll() {
        return queryRepository.findAll()
                .stream()
                .map(KitchenResponseMapper::toResponse)
                .toList();
    }
}