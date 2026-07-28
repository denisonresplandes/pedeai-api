package dev.denison.api.pedeai.application.kitchen;

import dev.denison.api.pedeai.application.kitchen.dto.CreateKitchenCommand;
import dev.denison.api.pedeai.application.kitchen.dto.DeleteKitchenCommand;
import dev.denison.api.pedeai.application.kitchen.dto.KitchenResponse;
import dev.denison.api.pedeai.application.kitchen.dto.UpdateKitchenCommand;
import dev.denison.api.pedeai.application.kitchen.exceptions.KitchenNameAlreadyExistsException;
import dev.denison.api.pedeai.application.kitchen.exceptions.KitchenNotFoundException;
import dev.denison.api.pedeai.application.shared.ValidationException;
import dev.denison.api.pedeai.domain.kitchen.Kitchen;
import dev.denison.api.pedeai.domain.kitchen.KitchenRepository;
import dev.denison.api.pedeai.domain.kitchen.vo.KitchenId;
import dev.denison.api.pedeai.domain.kitchen.vo.KitchenName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KitchenCommandHandlerTest {

    @Mock
    private KitchenRepository repository;

    @InjectMocks
    private KitchenCommandHandler commandHandler;

    @Test
    @DisplayName("Deve criar uma cozinha com sucesso quando o nome for único.")
    void shouldCreateKitchenSuccessfully() {
        // Arrange
        CreateKitchenCommand command = new CreateKitchenCommand("Italian Food");
        KitchenName kitchenName = new KitchenName(command.name());
        // simula objeto salvo.
        Kitchen savedKitchen = Kitchen.restore(new KitchenId(1L), kitchenName);

        // Simulando o comportamento do mock do repositório
        when(repository.existsByName(any(KitchenName.class))).thenReturn(false);
        when(repository.save(any(Kitchen.class))).thenReturn(savedKitchen);

        // Act
        KitchenResponse response = commandHandler.create(command);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Italian Food", response.name());

        verify(repository, times(1)).existsByName(any(KitchenName.class));
        verify(repository, times(1)).save(any(Kitchen.class));
    }

    @Test
    @DisplayName("Deve lançar KitchenNameAlreadyExistsException quando o nome da cozinha já existir")
    void shouldThrowExceptionWhenNameExists() {
        // Arrange
        CreateKitchenCommand command = new CreateKitchenCommand("Japanese Food");
        when(repository.existsByName(any(KitchenName.class))).thenReturn(true);

        // Act & Assert
        assertThrows(KitchenNameAlreadyExistsException.class, () -> {
            commandHandler.create(command);
        });

        // Garante que o fluxo barrou na validação e NUNCA tentou salvar no banco
        verify(repository, times(1))
                .existsByName(any(KitchenName.class));
        verify(repository, never()).save(any(Kitchen.class));
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando o comando de criação for nulo.")
    void shouldThrowExceptionWhenCommandIsNull() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            commandHandler.create(null);
        });

        assertEquals("The create kitchen command cannot be null.", exception.getMessage());
        verifyNoInteractions(repository); // O repositório sequer deve ser tocado.
    }

    @Test
    @DisplayName("Deve atualizar o nome da cozinha com sucesso quando a cozinha existir e o novo nome for único.")
    void shouldUpdateKitchenSuccessfully() {
        // ARRANGE
        // Cria a intenção do usuário (Mudar o ID 1 para "New Italian Name")
        UpdateKitchenCommand command = new UpdateKitchenCommand(1L, "New Italian Name");

        // Fabrica o estado ATUAL da cozinha como ela viria do banco hoje ("Old Name")
        Kitchen existingKitchen = Kitchen.restore(new KitchenId(1L), new KitchenName("Old Name"));

        // Programa os Mocks:
        // "Quando buscar pelo ID 1, devolva a cozinha que está com o nome 'Old Name'"
        when(repository.findById(any(KitchenId.class))).thenReturn(Optional.of(existingKitchen));

        // Quando checar se o novo nome já existe em outra linha do banco, retorna false
        when(repository.existsByName(any(KitchenName.class))).thenReturn(false);

        // Quando chamar o .save(), devolva a própria cozinha
        when(repository.save(any(Kitchen.class))).thenReturn(existingKitchen);

        // ACT
        // Dispara o método_ do Handler.
        // Vai ver que "Old Name" é diferente de "New Italian Name" e chamará as regras.
        KitchenResponse response = commandHandler.update(command);

        // ASSERT
        // Verifica se o DTO de saída foi preenchido com as alterações feitas pelo agregado
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("New Italian Name", response.name()); // novo nome

        // Verifica se o Handler cumpriu toda a esteira obrigatória de persistência
        verify(repository, times(1)).findById(any(KitchenId.class));
        verify(repository, times(1)).existsByName(any(KitchenName.class));
        verify(repository, times(1)).save(any(Kitchen.class));
    }

    @Test
    @DisplayName("Deve excluir a cozinha com sucesso quando a cozinha existir.")
    void shouldDeleteKitchenSuccessfully() {
        // ARRANGE
        DeleteKitchenCommand command = new DeleteKitchenCommand(1L);
        Kitchen existingKitchen = Kitchen.restore(
                new KitchenId(1L),
                new KitchenName("To Be Deleted")
        );
        when(repository.findById(any(KitchenId.class))).thenReturn(Optional.of(existingKitchen));

        // ACT
        commandHandler.delete(command);

        // ASSERT
        verify(repository, times(1)).findById(any(KitchenId.class));
        verify(repository, times(1)).delete(any(Kitchen.class));
    }

    @Test
    @DisplayName("Deve lançar KitchenNotFoundException ao excluir uma cozinha inexistente.")
    void shouldThrowExceptionWhenDeletingNonExistingKitchen() {
        // ARRANGE
        DeleteKitchenCommand command = new DeleteKitchenCommand(99L);
        when(repository.findById(any(KitchenId.class))).thenReturn(Optional.empty());

        // ACT
        assertThrows(KitchenNotFoundException.class, () -> {
            commandHandler.delete(command);
        });

        // ASSERT
        verify(repository, times(1)).findById(any(KitchenId.class));
        verify(repository, never()).delete(any(Kitchen.class));
    }
}
