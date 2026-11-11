package cl.textilcircular.api.core.model.business;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Predefined rejection reasons offered by the app; {@code label} is the text it shows. */
@Getter
@RequiredArgsConstructor
public enum RejectionReason {
    INCOMPLETE_DATA("Datos incompletos o erróneos"),
    UNCLEAR_DESCRIPTION("Descripción confusa o muy breve"),
    INAPPROPRIATE_IMAGES("Foto o imágenes inadecuadas"),
    NOT_CIRCULAR("No cumple con enfoque de economía circular"),
    OTHER("Otro motivo");

    private final String label;
}
