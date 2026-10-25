package dev.denison.api.pedeai.core.application.kitchen;

import dev.denison.api.pedeai.core.application.kitchen.dto.KitchenResponse;
import dev.denison.api.pedeai.core.domain.kitchen.vo.KitchenId;

import java.util.List;
import java.util.Optional;

/*
 * Porta de LEITURA (lado Query do CQRS). Mora na aplicação, não no domínio: ela devolve o modelo de
 * leitura (KitchenResponse), que é um DTO da aplicação, e o domínio nunca a usa. Se estivesse em
 * core.domain, o domínio passaria a importar um tipo da aplicação, invertendo a regra de dependência.
 *
 * Os critérios de busca são valores simples (Long, String) e não VOs: um termo de busca não é um
 * "nome válido" (ex.: a busca por "a" deve dar "não encontrado", não "nome curto demais").
 * Já o repositório de ESCRITA (KitchenRepository, no domínio) fala em agregado e VOs.
 */
public interface KitchenQueryRepository {

    Optional<KitchenResponse> findById(Long id);

    Optional<KitchenResponse> findByName(String name);

    List<KitchenResponse> findAll();

    boolean existsById(KitchenId id);
}
