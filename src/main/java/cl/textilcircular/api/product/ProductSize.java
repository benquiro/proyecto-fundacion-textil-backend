package cl.textilcircular.api.product;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** {@code label} is the text the app shows. */
@Getter
@RequiredArgsConstructor
public enum ProductSize {
    XS("XS"),
    S("S"),
    M("M"),
    L("L"),
    XL("XL"),
    ONE_SIZE("Única");

    private final String label;
}
