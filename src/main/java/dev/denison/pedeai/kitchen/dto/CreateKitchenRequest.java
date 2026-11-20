package dev.denison.pedeai.kitchen.dto;

import dev.denison.pedeai.kitchen.Kitchen;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateKitchenRequest(
        @NotBlank(message = "{kitchen.name.required}")
        @Size(message = "{kitchen.name.size}",
                min = Kitchen.NAME_MIN_LENGTH, max = Kitchen.NAME_MAX_LENGTH)
        String name
) {

    public CreateKitchenRequest {
        // Apara como a entidade (Kitchen.changeName), para que
        // @NotBlank e @Size meçam o mesmo texto que ela mede.
        name = name == null ? null : name.trim();
    }
}
