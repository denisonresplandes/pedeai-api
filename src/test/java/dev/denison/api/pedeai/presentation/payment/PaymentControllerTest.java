package dev.denison.api.pedeai.presentation.payment;

import dev.denison.api.pedeai.core.application.payment.PaymentCommandHandler;
import dev.denison.api.pedeai.core.application.payment.exceptions.UnsupportedPaymentMethodException;
import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * @WebMvcTest sobe só a camada web (incluindo o GlobalExceptionHandler); o handler do core é mockado.
 */
@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentCommandHandler commandHandler;

    @Test
    @DisplayName("Deve devolver 202 Accepted ao receber um pagamento válido.")
    void shouldReturnAcceptedWhenRequestIsValid() throws Exception {
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 10.50, "method": "PIX"}
                                """))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("Deve devolver 400 (e não 500) quando o método de pagamento não é suportado.")
    void shouldReturnBadRequestWhenPaymentMethodIsNotSupported() throws Exception {
        // Sem um handler mapeado para a exceção, o Spring a deixa subir e o cliente recebe 500.
        doThrow(new UnsupportedPaymentMethodException(PaymentMethod.PIX))
                .when(commandHandler).processPayment(any());

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 10.50, "method": "PIX"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unsupported payment method: PIX"));
    }
}
