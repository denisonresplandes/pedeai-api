package dev.denison.pedeai.kitchen;

import dev.denison.pedeai.kitchen.exception.KitchenNameAlreadyExistsException;
import dev.denison.pedeai.kitchen.exception.KitchenNotFoundException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Casos de uso de {@link Kitchen}: coordena a entidade e o {@link KitchenRepository} e controla a transação.
 * <p>
 * Os métodos devolvem a entidade. Quem a converte em DTO é o controller.
 */
@Service
@Transactional
public class KitchenService {

    private final KitchenRepository repository;

    KitchenService(KitchenRepository repository) {
        this.repository = repository;
    }

    /**
     * Cria uma cozinha. A entidade apara e valida o nome; aqui se confere, já com o nome normalizado, se ele está
     * em uso (sem diferenciar a caixa).
     * <p>
     * A checagem sozinha não fecha a corrida entre duas requisições: as duas podem ver o nome livre antes de qualquer
     * commit. Quem decide é a constraint UNIQUE do banco. O {@code saveAndFlush} faz o INSERT aqui dentro, e a
     * violação da constraint do nome vira também {@link KitchenNameAlreadyExistsException}. As outras violações de
     * integridade seguem adiante (o {@code GlobalExceptionHandler} as responde com um 409 genérico).
     *
     * @throws KitchenNameAlreadyExistsException se já existe uma cozinha com esse nome
     */
    public Kitchen create(String name) {
        Kitchen kitchen = Kitchen.create(name);

        if (repository.existsByNameIgnoreCase(kitchen.getName())) {
            throw new KitchenNameAlreadyExistsException(kitchen.getName());
        }
        try {
            return repository.saveAndFlush(kitchen);
        }
        catch (DataIntegrityViolationException e) {
            if (isUniqueViolation(e)) {
                throw new KitchenNameAlreadyExistsException(kitchen.getName());
            }
            throw e;
        }
    }

    private boolean isUniqueViolation(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            // só a violação do tipo UNIQUE deve ser traduzida para 409
            // (as outras devem seguir adiante).
            if (cause instanceof ConstraintViolationException violation
                    && violation.getConstraintName() != null) {
                String constraintName = violation.getConstraintName();
                if (constraintName.contains(Kitchen.UNIQUE_NAME)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @throws KitchenNotFoundException se não existe uma cozinha com esse id
     */
    @Transactional(readOnly = true)
    public Kitchen findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new KitchenNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Kitchen> findAll() {
        return repository.findAll();
    }
}
