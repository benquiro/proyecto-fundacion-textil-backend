package cl.textilcircular.api.core.validation;

import cl.textilcircular.api.core.error.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidatorsTest {

    @Test
    void textIsTrimmedAndBounded() {
        assertThat(Validators.text("name", "  Hilos  ", 10, "el nombre")).isEqualTo("Hilos");
        assertThatThrownBy(() -> Validators.text("name", "   ", 10, "el nombre"))
                .isInstanceOf(ValidationException.class).hasMessage("Ingresa el nombre")
                .extracting("field").isEqualTo("name");
        assertThatThrownBy(() -> Validators.text("name", "x".repeat(11), 10, "el nombre"))
                .hasMessage("El nombre admite hasta 10 caracteres");
    }

    @Test
    void blankOptionalTextBecomesNull() {
        assertThat(Validators.optionalText("alias", "  ", 40, "el alias")).isNull();
        assertThat(Validators.optionalText("alias", " Ana ", 40, "el alias")).isEqualTo("Ana");
    }

    @Test
    void emailIsNormalizedAndChecked() {
        assertThat(Validators.email("  Ana@Mail.CL ")).isEqualTo("ana@mail.cl");
        assertThatThrownBy(() -> Validators.email("")).hasMessage("Ingresa tu correo");
        assertThatThrownBy(() -> Validators.email("ana@mail")).hasMessage("Ingresa un correo válido");
    }

    @Test
    void passwordNeedsEightCharactersWithLettersAndDigits() {
        assertThat(Validators.password("Demo1234")).isEqualTo("Demo1234");
        assertThatThrownBy(() -> Validators.password("abcdefgh"))
                .hasMessage("La contraseña debe tener 8 o más caracteres, con letras y números");
        assertThatThrownBy(() -> Validators.password("Ab1"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rutIsNormalizedAndItsVerifierDigitChecked() {
        assertThat(Validators.rut("12.345.678-5")).isEqualTo("12345678-5");
        assertThat(Validators.rut("123456785")).isEqualTo("12345678-5");
        assertThat(Validators.rut("10.000.013-k")).isEqualTo("10000013-K");
        assertThatThrownBy(() -> Validators.rut("12345678-9"))
                .hasMessage("El dígito verificador del RUT no coincide");
        assertThatThrownBy(() -> Validators.rut("abc"))
                .hasMessage("Ingresa un RUT válido, por ejemplo 12345678-5");
    }

    @Test
    void imageUrlsMustBeWebOrLocalMedia() {
        assertThat(Validators.url("photoUrl", " https://images.unsplash.com/a.jpg ", true, "la foto"))
                .isEqualTo("https://images.unsplash.com/a.jpg");
        assertThat(Validators.url("photoUrl", "/media/0b9f.png", true, "la foto")).isEqualTo("/media/0b9f.png");
        assertThat(Validators.url("photoUrl", " ", false, "la foto")).isNull();
        for (String bad : new String[]{"javascript:alert(1)", "ftp://x/y.png", "data:image/png;base64,AA", "//evil.com/a.png", "/etc/passwd"}) {
            assertThatThrownBy(() -> Validators.url("photoUrl", bad, true, "la foto"))
                    .isInstanceOf(ValidationException.class).hasMessage("Ingresa un enlace válido para la foto");
        }
        assertThatThrownBy(() -> Validators.url("photoUrl", null, true, "la foto")).hasMessage("Ingresa la foto");
    }

    @Test
    void priceCannotBeNegative() {
        assertThat(Validators.price(null)).isNull();
        assertThat(Validators.price(0L)).isZero();
        assertThatThrownBy(() -> Validators.price(-1L)).hasMessage("El precio no puede ser negativo");
    }
}
