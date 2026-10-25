package dev.denison.api.pedeai.infrastructure.persistence.kitchen;

import dev.denison.api.pedeai.core.application.kitchen.dto.KitchenResponse;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/*
 * ATENÇÃO aos nomes: não use findById / findAll / existsById (os nomes do CRUD) nestes métodos.
 * O Spring Data roteia um método com o mesmo nome e parâmetros de um método do CRUD para a
 * implementação base, que devolve a ENTIDADE, e não faz a consulta derivada com a projeção. Como o
 * tipo genérico é apagado em execução, o erro só aparece depois, como ClassCastException
 * (KitchenEntity -> KitchenResponse) quando o valor é usado. Por isso os nomes têm uma palavra
 * extra entre "find" e "By" (o Spring Data ignora esse trecho ao montar a consulta) e o findAll é
 * escrito como findBy().
 */
public interface SpringDataKitchenQueryRepository extends Repository<KitchenEntity, Long> {

    // Não especifica nenhuma propriedade depois de By. Equivale ao findAll.
    List<KitchenResponse> findBy();

    Optional<KitchenResponse> findResponseById(Long id);

    Optional<KitchenResponse> findResponseByNameIgnoreCase(String name);

    boolean existsKitchenById(Long id);
}
