package dev.denison.api.pedeai.presentation.restaurant.dto;

import dev.denison.api.pedeai.core.application.restaurant.dto.CreateRestaurantCommand;
import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateRestaurantRequest(
        @NotBlank(message = "{restaurant.name.required}")
        @Size(min = 2, message = "{restaurant.name.size}")
        String name,

        /*
         * O texto entre chaves é uma CHAVE de messages.properties, resolvida pelo Bean Validation.
         * Se a chave não existir (ex.: "{kitchen.id.require}", sem o "d"), nenhum erro de
         * compilação aparece: o cliente apenas recebe o texto literal "{chave}".
         */
        @NotNull(message = "{kitchen.id.required}")
        @Positive(message = "{kitchen.id.positive}")
        Long kitchenId,

        Set<PaymentMethod> paymentMethods
) {
    public CreateRestaurantCommand toCommand() {
        return new CreateRestaurantCommand(name, kitchenId, paymentMethods);
    }
}
