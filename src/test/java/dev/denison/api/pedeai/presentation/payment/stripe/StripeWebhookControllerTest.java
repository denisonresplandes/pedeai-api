package dev.denison.api.pedeai.presentation.payment.stripe;

import dev.denison.api.pedeai.core.application.payment.PaymentCommandHandler;
import dev.denison.api.pedeai.core.application.payment.dto.ConfirmPaymentCommand;
import dev.denison.api.pedeai.core.application.payment.exceptions.PaymentNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * @WebMvcTest sobe só a camada web; o handler do core é mockado.
 *
 * As anotações do StripeWebhookPayload (@NotNull, @Positive) só são executadas se o
 * parâmetro do controller tiver @Valid. Sem ele elas são decorativas: o payload inválido
 * passa direto e estoura mais adiante (NPE -> 500). Por isso, além do status HTTP, estes
 * testes verificam que o handler do core NUNCA é chamado com um payload inválido.
 */
@WebMvcTest(StripeWebhookController.class)
class StripeWebhookControllerTest {

    private static final String URL = "/webhooks/stripe";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentCommandHandler commandHandler;

    @Test
    @DisplayName("Deve devolver 400 (e não 500) quando o payload não informa o status.")
    void shouldReturnBadRequestWhenStatusIsMissing() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentId": 1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status").value("The payment status cannot be null"));

        verify(commandHandler, never()).confirmPaymentFromWebhook(any());
    }

    @Test
    @DisplayName("Deve devolver 400 quando o paymentId não for positivo.")
    void shouldReturnBadRequestWhenPaymentIdIsNotPositive() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "SUCCESS", "paymentId": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.paymentId").value("The payment id must be a positive number"));

        verify(commandHandler, never()).confirmPaymentFromWebhook(any());
    }

    @Test
    @DisplayName("Deve devolver 400 quando o paymentId não for informado.")
    void shouldReturnBadRequestWhenPaymentIdIsMissing() throws Exception {
        // @Positive aceita null (null é "válido" para quase todas as constraints); só @NotNull barra.
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "SUCCESS"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.paymentId").value("The payment id cannot be null"));

        verify(commandHandler, never()).confirmPaymentFromWebhook(any());
    }

    @Test
    @DisplayName("Deve confirmar o pagamento com sucesso e responder 200 quando o status for SUCCESS.")
    void shouldConfirmPaymentWhenStatusIsSuccess() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "SUCCESS", "paymentId": 7}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<ConfirmPaymentCommand> captor = ArgumentCaptor.forClass(ConfirmPaymentCommand.class);
        verify(commandHandler).confirmPaymentFromWebhook(captor.capture());
        assertEquals(7L, captor.getValue().paymentId());
        assertTrue(captor.getValue().isSuccess());
    }

    @Test
    @DisplayName("Deve devolver 404 com a mensagem específica quando o pagamento do webhook não existe.")
    void shouldReturnNotFoundWhenPaymentDoesNotExist() throws Exception {
        doThrow(new PaymentNotFoundException(999L))
                .when(commandHandler).confirmPaymentFromWebhook(any());

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "SUCCESS", "paymentId": 999}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Payment with id 999 not found."));
    }

    @Test
    @DisplayName("Deve repassar isSuccess = false ao handler quando o status for CANCELLED.")
    void shouldReportFailureWhenStatusIsCancelled() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "CANCELLED", "paymentId": 7}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<ConfirmPaymentCommand> captor = ArgumentCaptor.forClass(ConfirmPaymentCommand.class);
        verify(commandHandler).confirmPaymentFromWebhook(captor.capture());
        assertFalse(captor.getValue().isSuccess());
    }
}
