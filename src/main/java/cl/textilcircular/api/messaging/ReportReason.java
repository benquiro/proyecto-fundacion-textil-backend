package cl.textilcircular.api.messaging;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Reasons offered by the app's report dialog; {@code label} is the text it shows. */
@Getter
@RequiredArgsConstructor
public enum ReportReason {
    SPAM("Spam o publicidad"),
    HARASSMENT("Acoso o lenguaje ofensivo"),
    SCAM("Intento de estafa"),
    OTHER("Otro motivo");

    private final String label;
}
