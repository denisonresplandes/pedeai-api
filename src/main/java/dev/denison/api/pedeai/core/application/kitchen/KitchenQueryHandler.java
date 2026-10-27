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
    
    /*
     * Filtro por nome: devolve sempre uma LISTA, como o findAll. Não achar nenhum resultado não é
     * erro (lista vazia); o 404 fica para um recurso específico (findById). O nome é único, então a
     * lista tem 0 ou 1 elemento, e a porta continua devolvendo Optional.
     */
    public List<KitchenResponse> findByName(String name) {
        return queryRepository.findByName(name)
                .map(List::of)
                .orElseGet(List::of);
    }
    
    public List<KitchenResponse> findAll() {
        return queryRepository.findAll();
    }
}