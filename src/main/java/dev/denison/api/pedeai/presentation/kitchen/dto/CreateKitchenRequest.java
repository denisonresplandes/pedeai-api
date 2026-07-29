package dev.denison.api.pedeai.presentation.kitchen.dto;

import dev.denison.api.pedeai.core.application.kitchen.dto.CreateKitchenCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateKitchenRequest(
        @NotBlank(message = "{kitchen.name.required}")
        @Size(min = 2, message = "{kitchen.name.size}")
        String name
) {
    public CreateKitchenCommand toCommand() {
        return new CreateKitchenCommand(this.name);
    }
}
