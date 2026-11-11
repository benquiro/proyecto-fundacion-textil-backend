package cl.textilcircular.api.core.model.product;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** {@code label} is the text the app shows. */
@Getter
@RequiredArgsConstructor
public enum OfferingType {
    MADE_TO_ORDER("Confección a pedido"),
    REPAIR("Reparación");

    private final String label;
}
