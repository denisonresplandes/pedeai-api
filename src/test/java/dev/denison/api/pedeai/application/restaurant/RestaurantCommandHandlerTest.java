package dev.denison.api.pedeai.application.restaurant;

import dev.denison.api.pedeai.core.application.kitchen.exceptions.KitchenNotFoundException;
import dev.denison.api.pedeai.core.application.restaurant.RestaurantCommandHandler;
import dev.denison.api.pedeai.core.application.restaurant.dto.CreateRestaurantCommand;
import dev.denison.api.pedeai.core.application.restaurant.dto.DeleteRestaurantCommand;
import dev.denison.api.pedeai.core.application.restaurant.dto.RestaurantResponse;
import dev.denison.api.pedeai.core.application.restaurant.dto.UpdateRestaurantCommand;
import dev.denison.api.pedeai.core.application.restaurant.exceptions.RestaurantNameAlreadyExistsException;
import dev.denison.api.pedeai.core.application.restaurant.exceptions.RestaurantNotFoundException;
import dev.denison.api.pedeai.core.application.shared.ValidationException;
import dev.denison.api.pedeai.core.domain.kitchen.KitchenQueryRepository;
import dev.denison.api.pedeai.core.domain.kitchen.vo.KitchenId;
import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;
import dev.denison.api.pedeai.core.domain.restaurant.Restaurant;
import dev.denison.api.pedeai.core.domain.restaurant.RestaurantRepository;
import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantId;
import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantCommandHandlerTest {

    @Mock
    private RestaurantRepository repository;

    @Mock
    private KitchenQueryRepository kitchenQueryRepository;

    @InjectMocks
    private RestaurantCommandHandler commandHandler;

    @Test
    @DisplayName("Deve criar um restaurante com sucesso quando o nome for único e a cozinha existir.")
    void shouldCreateRestaurantSuccessfully() {
        CreateRestaurantCommand command = new CreateRestaurantCommand("Ilha Verde", 1L, Set.of(PaymentMethod.PIX));
        Restaurant savedRestaurant = Restaurant.restore(
                new RestaurantId(1L), new RestaurantName("Ilha Verde"), new KitchenId(1L));
        savedRestaurant.addPaymentMethod(PaymentMethod.PIX);

        when(repository.existsByName(any(RestaurantName.class))).thenReturn(false);
        when(kitchenQueryRepository.existsById(any(KitchenId.class))).thenReturn(true);
        when(repository.save(any(Restaurant.class))).thenReturn(savedRestaurant);

        RestaurantResponse response = commandHandler.create(command);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Ilha Verde", response.name());
        assertEquals(1L, response.kitchenId());
        assertEquals(Set.of(PaymentMethod.CASH, PaymentMethod.PIX), response.paymentMethods());
        verify(repository, times(1)).existsByName(any(RestaurantName.class));
        verify(kitchenQueryRepository, times(1)).existsById(any(KitchenId.class));
        verify(repository, times(1)).save(any(Restaurant.class));
    }

    @Test
    @DisplayName("Deve lançar RestaurantNameAlreadyExistsException quando o nome do restaurante já existir.")
    void shouldThrowExceptionWhenNameExists() {
        CreateRestaurantCommand command = new CreateRestaurantCommand("Ilha Verde", 1L, Set.of());
        when(repository.existsByName(any(RestaurantName.class))).thenReturn(true);

        assertThrows(RestaurantNameAlreadyExistsException.class, () -> commandHandler.create(command));

        verifyNoInteractions(kitchenQueryRepository);
        verify(repository, never()).save(any(Restaurant.class));
    }

    @Test
    @DisplayName("Deve lançar KitchenNotFoundException quando a cozinha informada não existir.")
    void shouldThrowExceptionWhenKitchenDoesNotExist() {
        CreateRestaurantCommand command = new CreateRestaurantCommand("Ilha Verde", 99L, Set.of());
        when(repository.existsByName(any(RestaurantName.class))).thenReturn(false);
        when(kitchenQueryRepository.existsById(any(KitchenId.class))).thenReturn(false);

        assertThrows(KitchenNotFoundException.class, () -> commandHandler.create(command));

        verify(repository, never()).save(any(Restaurant.class));
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando o comando de criação for nulo.")
    void shouldThrowExceptionWhenCreateCommandIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> commandHandler.create(null));

        assertEquals("The create restaurant command cannot be null.", exception.getMessage());
        verifyNoInteractions(repository, kitchenQueryRepository);
    }

    @Test
    @DisplayName("Deve atualizar o nome do restaurante com sucesso quando ele existir e o novo nome for único.")
    void shouldUpdateRestaurantSuccessfully() {
        UpdateRestaurantCommand command = new UpdateRestaurantCommand(1L, "New Name");
        Restaurant existing = Restaurant.restore(
                new RestaurantId(1L), new RestaurantName("Old Name"), new KitchenId(1L));

        when(repository.findById(any(RestaurantId.class))).thenReturn(Optional.of(existing));
        when(repository.existsByName(any(RestaurantName.class))).thenReturn(false);
        when(repository.save(any(Restaurant.class))).thenReturn(existing);

        RestaurantResponse response = commandHandler.update(command);

        assertEquals(1L, response.id());
        assertEquals("New Name", response.name());
        verify(repository, times(1)).existsByName(any(RestaurantName.class));
        verify(repository, times(1)).save(existing);
    }

    @Test
    @DisplayName("Não deve validar unicidade quando o novo nome diferir do atual apenas pela caixa.")
    void shouldSkipUniquenessCheckWhenNameDiffersOnlyByCase() {
        UpdateRestaurantCommand command = new UpdateRestaurantCommand(1L, "ilha verde");
        Restaurant existing = Restaurant.restore(
                new RestaurantId(1L), new RestaurantName("Ilha Verde"), new KitchenId(1L));

        when(repository.findById(any(RestaurantId.class))).thenReturn(Optional.of(existing));
        when(repository.save(any(Restaurant.class))).thenReturn(existing);

        RestaurantResponse response = commandHandler.update(command);

        assertEquals("ilha verde", response.name());
        verify(repository, never()).existsByName(any(RestaurantName.class));
        verify(repository, times(1)).save(existing);
    }

    @Test
    @DisplayName("Deve lançar RestaurantNameAlreadyExistsException ao atualizar para um nome já usado.")
    void shouldThrowExceptionWhenUpdatingToExistingName() {
        UpdateRestaurantCommand command = new UpdateRestaurantCommand(1L, "Quero Mais");
        Restaurant existing = Restaurant.restore(
                new RestaurantId(1L), new RestaurantName("Ilha Verde"), new KitchenId(1L));

        when(repository.findById(any(RestaurantId.class))).thenReturn(Optional.of(existing));
        when(repository.existsByName(any(RestaurantName.class))).thenReturn(true);

        assertThrows(RestaurantNameAlreadyExistsException.class, () -> commandHandler.update(command));

        verify(repository, never()).save(any(Restaurant.class));
    }

    @Test
    @DisplayName("Deve lançar RestaurantNotFoundException ao atualizar um restaurante inexistente.")
    void shouldThrowExceptionWhenUpdatingNonExistingRestaurant() {
        UpdateRestaurantCommand command = new UpdateRestaurantCommand(99L, "Any Name");
        when(repository.findById(any(RestaurantId.class))).thenReturn(Optional.empty());

        assertThrows(RestaurantNotFoundException.class, () -> commandHandler.update(command));

        verify(repository, never()).save(any(Restaurant.class));
    }

    @Test
    @DisplayName("Deve excluir o restaurante com sucesso quando ele existir.")
    void shouldDeleteRestaurantSuccessfully() {
        DeleteRestaurantCommand command = new DeleteRestaurantCommand(1L);
        when(repository.existsById(any(RestaurantId.class))).thenReturn(true);

        commandHandler.delete(command);

        verify(repository, times(1)).existsById(any(RestaurantId.class));
        verify(repository, times(1)).deleteById(any(RestaurantId.class));
        verify(repository, never()).findById(any(RestaurantId.class));
    }

    @Test
    @DisplayName("Deve lançar RestaurantNotFoundException ao excluir um restaurante inexistente.")
    void shouldThrowExceptionWhenDeletingNonExistingRestaurant() {
        DeleteRestaurantCommand command = new DeleteRestaurantCommand(99L);
        when(repository.existsById(any(RestaurantId.class))).thenReturn(false);

        assertThrows(RestaurantNotFoundException.class, () -> commandHandler.delete(command));

        verify(repository, never()).deleteById(any(RestaurantId.class));
    }

    @Test
    @DisplayName("Deve lançar ValidationException quando o comando de exclusão for nulo.")
    void shouldThrowExceptionWhenDeleteCommandIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> commandHandler.delete(null));

        assertEquals("The delete restaurant command cannot be null.", exception.getMessage());
        verifyNoInteractions(repository);
    }
}
