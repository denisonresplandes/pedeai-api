package dev.denison.api.pedeai.core.application.kitchen.dto;

/*
 * O Command só carrega a intenção do usuário; não valida nada.
 * Quem barra entrada inválida são duas camadas: o Request (mensagem amigável por campo, na borda
 * da API) e o VO (KitchenName, a invariante do domínio, que vale para qualquer chamador).
 * Uma terceira checagem aqui só repetia essas regras e podia divergir delas.
 */
public record CreateKitchenCommand(
        String name
) {
}
