package dev.denison.api.pedeai.core.domain.restaurant.vo;

import dev.denison.api.pedeai.core.domain.shared.DomainException;

public record RestaurantName(
        String value
) {
    /*
     * Fonte ÚNICA dos limites do nome (usada também pelo @Size do Request e pelo
     * length da coluna na Entity). Mesmo raciocínio de KitchenName.
     */
    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 50;

    public RestaurantName {
        // requireNonBlank já faz o trim: o tamanho é medido sobre o texto "limpo".
        value = DomainException.requireNonBlank(
                value,
                "Restaurant name cannot be empty or blank."
        );
        if (value.length() < MIN_LENGTH) {
            throw new DomainException(
                    "Restaurant name must be at least %d characters long.".formatted(MIN_LENGTH)
            );
        }
        if (value.length() > MAX_LENGTH) {
            throw new DomainException(
                    "Restaurant name must be at most %d characters long.".formatted(MAX_LENGTH)
            );
        }
    }

    /*
     * Dois nomes que diferem só pela caixa são o MESMO nome para a regra de unicidade
     * ("Ilha Verde" == "ilha verde"), o mesmo critério do banco (existsByNameIgnoreCase).
     * Independe do locale padrão da JVM: veja KitchenName.equalsIgnoreCase.
     */
    public boolean equalsIgnoreCase(RestaurantName other) {
        return other != null && value.equalsIgnoreCase(other.value);
    }
}
