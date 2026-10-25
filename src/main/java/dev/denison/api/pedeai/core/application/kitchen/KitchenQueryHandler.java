package dev.denison.api.pedeai.core.application.kitchen;

import dev.denison.api.pedeai.core.application.kitchen.dto.KitchenResponse;
import dev.denison.api.pedeai.core.application.kitchen.exceptions.KitchenNotFoundException;

import java.util.List;

public class KitchenQueryHandler {

    private final KitchenQueryRepository queryRepository;

    public KitchenQueryHandler(KitchenQueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    public KitchenResponse findById(Long id) {
        return queryRepository.findById(id)
                .orElseThrow(() -> new KitchenNotFoundException(id));
    }
    
    public KitchenResponse findByName(String name) {
        return queryRepository.findByName(name)
                .orElseThrow(() -> new KitchenNotFoundException(name));
    }
    
    public List<KitchenResponse> findAll() {
        return queryRepository.findAll();
    }
}