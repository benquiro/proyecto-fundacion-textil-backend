package cl.textilcircular.api.core.model.product;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** {@code label} is the text the app shows. */
@Getter
@RequiredArgsConstructor
public enum ProductCondition {
    NEW("Nuevo"),
    GOOD("Buen estado"),
    WITH_DETAILS("Con detalles");

    private final String label;
}
