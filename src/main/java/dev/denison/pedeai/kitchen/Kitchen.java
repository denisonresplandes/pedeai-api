package dev.denison.pedeai.kitchen;

import dev.denison.pedeai.shared.exception.BusinessException;
import jakarta.persistence.*;

/**
 * Cozinha, o tipo de culinária dos restaurantes (por exemplo, "Italiana").
 * <p>
 * É uma entidade JPA e também o modelo de negócio: a regra do nome mora aqui, por isso não há {@code setName}.
 * A mudança passa por {@link #changeName(String)}, então uma {@code Kitchen} que existe sempre tem um nome válido.
 */
@Entity
@Table(name = "tb_kitchen",
        uniqueConstraints = @UniqueConstraint(name = Kitchen.UNIQUE_NAME, columnNames = "name"))
public class Kitchen {

    static final String UNIQUE_NAME = "uk_kitchen_name";

    public static final int NAME_MIN_LENGTH = 2;
    public static final int NAME_MAX_LENGTH = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    Kitchen() {} // para uso do Hibernate

    private Kitchen(Long id, String name) {
        this.id = id;
        changeName(name);
    }

    public static Kitchen create(String name) {
        return new Kitchen(null, name);
    }

    public static Kitchen restore(Long id, String name) {
        BusinessException.requirePositive(
                id,
                "Kitchen id must be a positive number"
        );
        return new Kitchen(id, name);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Altera o nome, tirando os espaços das pontas. O tamanho mínimo e o máximo são medidos depois disso.
     *
     * @throws BusinessException se o nome for nulo, curto demais ou longo demais
     */
    public void changeName(String newName) {
        BusinessException.requireNonNull(newName, "Kitchen name is required");

        String trimmed = newName.trim();
        if (trimmed.length() < NAME_MIN_LENGTH || trimmed.length() > NAME_MAX_LENGTH) {
            throw new BusinessException("Kitchen name must have between " + NAME_MIN_LENGTH
                    + " and " + NAME_MAX_LENGTH + " characters");
        }
        this.name = trimmed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Kitchen kitchen)) {
            return false;
        }
        return getId() != null && getId().equals(kitchen.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
