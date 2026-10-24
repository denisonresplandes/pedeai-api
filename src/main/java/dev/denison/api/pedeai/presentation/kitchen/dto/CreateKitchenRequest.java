package dev.denison.api.pedeai.presentation.kitchen.dto;

import dev.denison.api.pedeai.core.application.kitchen.dto.CreateKitchenCommand;
import dev.denison.api.pedeai.core.domain.kitchen.vo.KitchenName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateKitchenRequest(
        @NotBlank(message = "{kitchen.name.required}")
        @Size(min = KitchenName.MIN_LENGTH, max = KitchenName.MAX_LENGTH, message = "{kitchen.name.size}")
        String name
) {
    public CreateKitchenCommand toCommand() {
        return new CreateKitchenCommand(this.name);
    }
}
