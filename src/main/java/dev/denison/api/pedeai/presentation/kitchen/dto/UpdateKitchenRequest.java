package dev.denison.api.pedeai.presentation.kitchen.dto;

import dev.denison.api.pedeai.core.application.kitchen.dto.UpdateKitchenCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateKitchenRequest(
        @NotBlank(message = "The kitchen name cannot be empty")
        @Size(min = 2, message = "The kitchen name must be at least 2 characters long")
        String name
) {
    public UpdateKitchenCommand toCommand(Long id) {
        return new UpdateKitchenCommand(id, name);
    }
}
