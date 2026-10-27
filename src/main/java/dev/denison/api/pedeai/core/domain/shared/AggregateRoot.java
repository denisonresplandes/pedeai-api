package dev.denison.api.pedeai.core.domain.shared;

/*
 * Marca as raízes de agregado (Kitchen, Restaurant, Payment): só elas são carregadas e salvas
 * pelos repositórios, e qualquer mudança nos objetos internos passa por elas.
 * Está vazia de propósito. É o lugar natural para registrar eventos de domínio, se a etapa 19 do
 * TODO decidir por esse caminho; se não decidir, esta classe pode ser removida.
 */
public abstract class AggregateRoot {
}
