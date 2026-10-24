package dev.denison.api.pedeai.presentation.kitchen.dto;

import dev.denison.api.pedeai.core.application.kitchen.dto.UpdateKitchenCommand;
import dev.denison.api.pedeai.core.domain.kitchen.vo.KitchenName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record  UpdateKitchenRequest(
        @NotBlank(message = "{kitchen.name.required}")
        @Size(min = KitchenName.MIN_LENGTH, max = KitchenName.MAX_LENGTH, message = "{kitchen.name.size}")
        String name
) {
    public UpdateKitchenCommand toCommand(Long id) {
        return new UpdateKitchenCommand(id, name);
    }
}
