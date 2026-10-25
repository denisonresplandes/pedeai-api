package dev.denison.api.pedeai.infrastructure.persistence.kitchen;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataKitchenRepository extends JpaRepository<KitchenEntity, Long> {

    /*
     * IgnoreCase faz o Spring Data gerar "where upper(name) = upper(?)". Assim a regra
     * "nome único sem diferenciar maiúsculas" fica no código e não depende da collation do banco:
     * o MySQL (_ci) já ignora a caixa sozinho, mas o PostgreSQL e outros bancos não.
     * Mesmo critério do Restaurant (existsByNameIgnoreCase) e do KitchenCommandHandler.isEquals.
     */
    boolean existsByNameIgnoreCase(String name);
}
