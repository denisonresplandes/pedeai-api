package dev.denison.api.pedeai.application.kitchen;

import dev.denison.api.pedeai.core.application.kitchen.KitchenCommandHandler;
import dev.denison.api.pedeai.core.application.kitchen.dto.CreateKitchenCommand;
import dev.denison.api.pedeai.core.application.kitchen.dto.DeleteKitchenCommand;
import dev.denison.api.pedeai.core.application.kitchen.dto.KitchenResponse;
import dev.denison.api.pedeai.core.application.kitchen.dto.UpdateKitchenCommand;
import dev.denison.api.pedeai.core.application.kitchen.exceptions.KitchenInUseException;
import dev.denison.api.pedeai.core.application.kitchen.exceptions.KitchenNameAlreadyExistsException;
import dev.denison.api.pedeai.core.application.kitchen.exceptions.KitchenNotFoundException;
import dev.denison.api.pedeai.core.application.restaurant.RestaurantQueryRepository;
import dev.denison.api.pedeai.core.application.shared.ValidationException;
import dev.denison.api.pedeai.core.domain.kitchen.Kitchen;
import dev.denison.api.pedeai.core.domain.kitchen.KitchenRepository;
import dev.denison.api.pedeai.core.domain.kitchen.vo.KitchenId;
import dev.denison.api.pedeai.core.domain.kitchen.vo.KitchenName;
import dev.denison.api.pedeai.core.domain.shared.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KitchenCommandHandlerTest {

    @Mock
    private KitchenRepository repository;

    @Mock
    private RestaurantQueryRepository restaurantQueryRepository;

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
        when(restaurantQueryRepository.existsByKitchenId(any(KitchenId.class))).thenReturn(false);

        // ACT
        commandHandler.delete(command);

        // ASSERT
        verify(repository, times(1)).findById(any(KitchenId.class));
        verify(repository, times(1)).delete(any(Kitchen.class));
    }

    @Test
    @DisplayName("Deve lançar KitchenInUseException ao excluir uma cozinha com restaurantes vinculados.")
    void shouldThrowExceptionWhenDeletingKitchenInUse() {
        // ARRANGE
        DeleteKitchenCommand command = new DeleteKitchenCommand(1L);
        Kitchen existingKitchen = Kitchen.restore(new KitchenId(1L), new KitchenName("In Use"));
        when(repository.findById(any(KitchenId.class))).thenReturn(Optional.of(existingKitchen));
        when(restaurantQueryRepository.existsByKitchenId(any(KitchenId.class))).thenReturn(true);

        // ACT
        assertThrows(KitchenInUseException.class, () -> commandHandler.delete(command));

        // ASSERT
        verify(repository, never()).delete(any(Kitchen.class));
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

    @Test
    @DisplayName("Não deve validar unicidade quando o novo nome diferir do atual apenas pela caixa.")
    void shouldSkipUniquenessCheckWhenNameDiffersOnlyByCase() {
        UpdateKitchenCommand command = new UpdateKitchenCommand(1L, "indiana");
        Kitchen existing = Kitchen.restore(new KitchenId(1L), new KitchenName("Indiana"));
        when(repository.findById(any(KitchenId.class))).thenReturn(Optional.of(existing));
        when(repository.save(any(Kitchen.class))).thenReturn(existing);

        KitchenResponse response = commandHandler.update(command);

        assertEquals("indiana", response.name());
        verify(repository, never()).existsByName(any(KitchenName.class));
    }

    /*
     * Comparar nomes "sem diferenciar a caixa" não pode depender do idioma da máquina. Com o locale
     * turco, "I".toLowerCase() devolve "ı" (i sem ponto), então "Indiana" e "indiana" deixavam de
     * ser considerados o mesmo nome: renomear a cozinha só trocando a caixa virava um falso 409
     * ("já existe"), porque a checagem de unicidade encontrava a própria cozinha.
     */
    @Test
    @DisplayName("Não deve validar unicidade na mudança só de caixa mesmo com o locale padrão turco (tr-TR).")
    void shouldSkipUniquenessCheckWhenNameDiffersOnlyByCaseUnderTurkishLocale() {
        Locale original = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            UpdateKitchenCommand command = new UpdateKitchenCommand(1L, "indiana");
            Kitchen existing = Kitchen.restore(new KitchenId(1L), new KitchenName("Indiana"));
            when(repository.findById(any(KitchenId.class))).thenReturn(Optional.of(existing));
            when(repository.save(any(Kitchen.class))).thenReturn(existing);

            KitchenResponse response = commandHandler.update(command);

            assertEquals("indiana", response.name());
            verify(repository, never()).existsByName(any(KitchenName.class));
        } finally {
            Locale.setDefault(original);
        }
    }

    /*
     * Os Commands não validam mais nada (etapa 9): a entrada inválida é barrada pelo Request
     * (mensagem amigável, na borda da API) e pelos VOs (invariantes do domínio, valem para qualquer
     * chamador). Estes testes garantem que o VO continua protegendo e que isso acontece ANTES de
     * qualquer acesso ao repositório.
     */
    @Test
    @DisplayName("Deve lançar DomainException (do VO) quando o nome tem menos de 2 caracteres depois do trim.")
    void shouldRejectNameTooShortAfterTrim() {
        CreateKitchenCommand command = new CreateKitchenCommand(" a ");

        assertThrows(DomainException.class, () -> commandHandler.create(command));

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Deve lançar DomainException (do VO) ao atualizar com id não positivo.")
    void shouldRejectNonPositiveIdOnUpdate() {
        UpdateKitchenCommand command = new UpdateKitchenCommand(0L, "Valid Name");

        assertThrows(DomainException.class, () -> commandHandler.update(command));

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Deve lançar DomainException (do VO) ao excluir com id não positivo ou nulo.")
    void shouldRejectInvalidIdOnDelete() {
        DeleteKitchenCommand zero = new DeleteKitchenCommand(0L);
        DeleteKitchenCommand missing = new DeleteKitchenCommand(null);

        assertThrows(DomainException.class, () -> commandHandler.delete(zero));
        assertThrows(DomainException.class, () -> commandHandler.delete(missing));

        verifyNoInteractions(repository, restaurantQueryRepository);
    }
}
