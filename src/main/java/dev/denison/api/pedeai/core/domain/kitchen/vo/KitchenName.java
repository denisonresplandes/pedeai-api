package dev.denison.api.pedeai.core.domain.kitchen.vo;

import dev.denison.api.pedeai.core.domain.shared.DomainException;

public record KitchenName(
        String value
) {
    /*
     * Fonte ÚNICA dos limites do nome. Quem precisa dessa regra (@Size no Request,
     * length da coluna na Entity) lê estas constantes em vez de repetir o número,
     * assim mudar o limite é mudar uma linha só.
     */
    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 50;

    public KitchenName {
        // requireNonBlank já faz o trim: o tamanho é medido sobre o texto "limpo",
        // então "  a  " (1 caractere útil) é rejeitado e "  " + 50 caracteres + "  " é aceito.
        value = DomainException.requireNonBlank(
                value,
                "Kitchen name cannot be empty or blank."
        );
        if (value.length() < MIN_LENGTH) {
            throw new DomainException(
                    "Kitchen name must be at least %d characters long.".formatted(MIN_LENGTH)
            );
        }
        if (value.length() > MAX_LENGTH) {
            throw new DomainException(
                    "Kitchen name must be at most %d characters long.".formatted(MAX_LENGTH)
            );
        }
    }

    /*
     * Dois nomes que diferem só pela caixa são o MESMO nome para a regra de unicidade
     * ("Indiana" == "indiana"), o mesmo critério do banco (existsByNameIgnoreCase). O equals() do
     * record continua sendo o case-sensitive padrão.
     *
     * String.equalsIgnoreCase não depende do locale padrão da JVM.
     */
    public boolean equalsIgnoreCase(KitchenName other) {
        return other != null && value.equalsIgnoreCase(other.value);
    }
}
