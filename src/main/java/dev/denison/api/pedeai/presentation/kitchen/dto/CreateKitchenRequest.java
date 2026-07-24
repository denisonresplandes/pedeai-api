package dev.denison.api.pedeai.presentation.kitchen.dto;

import dev.denison.api.pedeai.application.kitchen.dto.CreateKitchenCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateKitchenRequest(
        @NotBlank(message = "The name cannot be empty")
        @Size(min = 2, message = "The name must be at least 2 characters long")
        String name
) {
    public CreateKitchenCommand toCommand() {
        return new CreateKitchenCommand(this.name);
    }
}
