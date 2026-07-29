package dev.denison.api.pedeai.presentation.kitchen.dto;

import dev.denison.api.pedeai.core.application.kitchen.dto.UpdateKitchenCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateKitchenRequest(
        @NotBlank(message = "{kitchen.name.required}")
        @Size(min = 2, message = "{kitchen.name.size}")
        String name
) {
    public UpdateKitchenCommand toCommand(Long id) {
        return new UpdateKitchenCommand(id, name);
    }
}
