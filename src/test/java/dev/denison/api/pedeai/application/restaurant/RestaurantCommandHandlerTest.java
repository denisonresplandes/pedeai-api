package dev.denison.api.pedeai.application.restaurant;

import dev.denison.api.pedeai.core.application.kitchen.KitchenQueryRepository;
import dev.denison.api.pedeai.core.application.kitchen.exceptions.KitchenNotFoundException;
import dev.denison.api.pedeai.core.application.restaurant.RestaurantCommandHandler;
import dev.denison.api.pedeai.core.application.restaurant.dto.CreateRestaurantCommand;
import dev.denison.api.pedeai.core.application.restaurant.dto.RestaurantResponse;
import dev.denison.api.pedeai.core.application.restaurant.dto.UpdateRestaurantCommand;
import dev.denison.api.pedeai.core.application.restaurant.exceptions.RestaurantNameAlreadyExistsException;
import dev.denison.api.pedeai.core.application.restaurant.exceptions.RestaurantNotFoundException;
import dev.denison.api.pedeai.core.application.shared.ValidationException;
import dev.denison.api.pedeai.core.domain.kitchen.vo.KitchenId;
import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;
import dev.denison.api.pedeai.core.domain.restaurant.Restaurant;
import dev.denison.api.pedeai.core.domain.restaurant.RestaurantRepository;
import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantId;
import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantName;
import dev.denison.api.pedeai.core.domain.shared.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Locale;
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
                new RestaurantId(1L), new RestaurantName("Ilha Verde"), new KitchenId(1L),
                Set.of(PaymentMethod.CASH, PaymentMethod.PIX));

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
    @DisplayName("Deve enviar ao repositório um restaurante com os métodos informados mais CASH.")
    void shouldPassPaymentMethodsToTheAggregate() {
        CreateRestaurantCommand command = new CreateRestaurantCommand("Ilha Verde", 1L, Set.of(PaymentMethod.PIX));
        when(repository.existsByName(any(RestaurantName.class))).thenReturn(false);
        when(kitchenQueryRepository.existsById(any(KitchenId.class))).thenReturn(true);
        when(repository.save(any(Restaurant.class))).thenAnswer(invocation -> {
            Restaurant toSave = invocation.getArgument(0);
            return Restaurant.restore(new RestaurantId(1L), toSave.getName(), toSave.getKitchenId(),
                    toSave.getPaymentMethods());
        });

        commandHandler.create(command);

        ArgumentCaptor<Restaurant> captor = ArgumentCaptor.forClass(Restaurant.class);
        verify(repository).save(captor.capture());
        assertEquals(Set.of(PaymentMethod.CASH, PaymentMethod.PIX), captor.getValue().getPaymentMethods());
    }

    @Test
    @DisplayName("Deve lançar DomainException e não salvar quando os métodos de pagamento contêm null.")
    void shouldThrowExceptionWhenPaymentMethodsContainNull() {
        Set<PaymentMethod> withNull = new HashSet<>();
        withNull.add(null);
        CreateRestaurantCommand command = new CreateRestaurantCommand("Ilha Verde", 1L, withNull);
        when(repository.existsByName(any(RestaurantName.class))).thenReturn(false);
        when(kitchenQueryRepository.existsById(any(KitchenId.class))).thenReturn(true);

        DomainException exception = assertThrows(DomainException.class, () -> commandHandler.create(command));

        assertEquals("Payment method cannot be null.", exception.getMessage());
        verify(repository, never()).save(any(Restaurant.class));
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
                new RestaurantId(1L), new RestaurantName("Old Name"), new KitchenId(1L),
                Set.of(PaymentMethod.CASH));

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
                new RestaurantId(1L), new RestaurantName("Ilha Verde"), new KitchenId(1L),
                Set.of(PaymentMethod.CASH));

        when(repository.findById(any(RestaurantId.class))).thenReturn(Optional.of(existing));
        when(repository.save(any(Restaurant.class))).thenReturn(existing);

        RestaurantResponse response = commandHandler.update(command);

        assertEquals("ilha verde", response.name());
        verify(repository, never()).existsByName(any(RestaurantName.class));
        verify(repository, times(1)).save(existing);
    }

    @Test
    @DisplayName("Não deve validar unicidade na mudança só de caixa mesmo com o locale padrão turco (tr-TR).")
    void shouldSkipUniquenessCheckWhenNameDiffersOnlyByCaseUnderTurkishLocale() {
        // Com o locale turco, "I".toLowerCase() devolve "ı" (i sem ponto): veja o teste equivalente
        // em KitchenCommandHandlerTest. "Ilha Verde" x "ilha verde" é exatamente o par que quebrava.
        Locale original = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            UpdateRestaurantCommand command = new UpdateRestaurantCommand(1L, "ilha verde");
            Restaurant existing = Restaurant.restore(
                    new RestaurantId(1L), new RestaurantName("Ilha Verde"), new KitchenId(1L),
                    Set.of(PaymentMethod.CASH));
            when(repository.findById(any(RestaurantId.class))).thenReturn(Optional.of(existing));
            when(repository.save(any(Restaurant.class))).thenReturn(existing);

            RestaurantResponse response = commandHandler.update(command);

            assertEquals("ilha verde", response.name());
            verify(repository, never()).existsByName(any(RestaurantName.class));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    @DisplayName("Deve lançar RestaurantNameAlreadyExistsException ao atualizar para um nome já usado.")
    void shouldThrowExceptionWhenUpdatingToExistingName() {
        UpdateRestaurantCommand command = new UpdateRestaurantCommand(1L, "Quero Mais");
        Restaurant existing = Restaurant.restore(
                new RestaurantId(1L), new RestaurantName("Ilha Verde"), new KitchenId(1L),
                Set.of(PaymentMethod.CASH));

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
        when(repository.existsById(any(RestaurantId.class))).thenReturn(true);

        commandHandler.delete(1L);

        verify(repository, times(1)).existsById(any(RestaurantId.class));
        verify(repository, times(1)).deleteById(any(RestaurantId.class));
        verify(repository, never()).findById(any(RestaurantId.class));
    }

    @Test
    @DisplayName("Deve lançar RestaurantNotFoundException ao excluir um restaurante inexistente.")
    void shouldThrowExceptionWhenDeletingNonExistingRestaurant() {
        when(repository.existsById(any(RestaurantId.class))).thenReturn(false);

        assertThrows(RestaurantNotFoundException.class, () -> commandHandler.delete(99L));

        verify(repository, never()).deleteById(any(RestaurantId.class));
    }

    /*
     * Os Commands não validam mais nada (etapa 9): a entrada inválida é barrada pelo Request
     * (mensagem amigável, na borda da API) e pelos VOs (invariantes do domínio, valem para qualquer
     * chamador). Estes testes garantem que o VO continua protegendo e que isso acontece ANTES de
     * qualquer acesso aos repositórios (nenhuma consulta ao banco com entrada inválida).
     */
    @Test
    @DisplayName("Deve lançar DomainException (do VO) quando o nome tem menos de 2 caracteres depois do trim.")
    void shouldRejectNameTooShortAfterTrim() {
        CreateRestaurantCommand command = new CreateRestaurantCommand(" a ", 1L, Set.of());

        assertThrows(DomainException.class, () -> commandHandler.create(command));

        verifyNoInteractions(repository, kitchenQueryRepository);
    }

    @Test
    @DisplayName("Deve lançar DomainException (do VO) antes de consultar o banco quando o kitchenId não é positivo.")
    void shouldRejectNonPositiveKitchenIdBeforeTouchingTheDatabase() {
        CreateRestaurantCommand command = new CreateRestaurantCommand("Ilha Verde", 0L, Set.of());

        assertThrows(DomainException.class, () -> commandHandler.create(command));

        verifyNoInteractions(repository, kitchenQueryRepository);
    }

    @Test
    @DisplayName("Deve criar o restaurante só com CASH quando os métodos de pagamento não são informados (null).")
    void shouldDefaultToCashOnlyWhenPaymentMethodsAreNotInformed() {
        // O JSON sem "paymentMethods" chega como null; não é erro, significa "nenhum além do CASH".
        CreateRestaurantCommand command = new CreateRestaurantCommand("Ilha Verde", 1L, null);
        when(repository.existsByName(any(RestaurantName.class))).thenReturn(false);
        when(kitchenQueryRepository.existsById(any(KitchenId.class))).thenReturn(true);
        when(repository.save(any(Restaurant.class))).thenAnswer(invocation -> {
            Restaurant toSave = invocation.getArgument(0);
            return Restaurant.restore(new RestaurantId(1L), toSave.getName(), toSave.getKitchenId(),
                    toSave.getPaymentMethods());
        });

        RestaurantResponse response = commandHandler.create(command);

        assertEquals(Set.of(PaymentMethod.CASH), response.paymentMethods());
    }

    @Test
    @DisplayName("Deve lançar DomainException (do VO) ao atualizar com id não positivo.")
    void shouldRejectNonPositiveIdOnUpdate() {
        UpdateRestaurantCommand command = new UpdateRestaurantCommand(0L, "Valid Name");

        assertThrows(DomainException.class, () -> commandHandler.update(command));

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Deve lançar DomainException (do VO) ao excluir com id não positivo ou nulo.")
    void shouldRejectInvalidIdOnDelete() {
        assertThrows(DomainException.class, () -> commandHandler.delete(0L));
        assertThrows(DomainException.class, () -> commandHandler.delete(null));

        verifyNoInteractions(repository);
    }
}
