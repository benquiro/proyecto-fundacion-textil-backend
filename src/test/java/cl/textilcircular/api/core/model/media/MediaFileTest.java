package cl.textilcircular.api.core.model.media;

import cl.textilcircular.api.core.error.ValidationException;
import org.junit.jupiter.api.Test;

import static cl.textilcircular.api.support.Fixtures.NOW;
import static cl.textilcircular.api.support.Fixtures.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaFileTest {

    @Test
    void imageGetsARandomIdAndAPathFromIt() {
        MediaFile file = MediaFile.register(customer(), "image/png", 1024, NOW);

        assertThat(file.getId()).isNotNull();
        assertThat(file.getStoragePath()).isEqualTo(file.getId() + ".png");
        assertThat(file.publicPath()).isEqualTo("/media/" + file.getId() + ".png");
        assertThat(file.isNew()).isTrue();
    }

    @Test
    void onlyJpegPngOrWebpUpToFiveMegabytes() {
        assertThatThrownBy(() -> MediaFile.register(customer(), "application/pdf", 10, NOW))
                .isInstanceOf(ValidationException.class).hasMessage("La imagen debe ser JPG, PNG o WEBP");
        assertThatThrownBy(() -> MediaFile.register(customer(), "image/jpeg", 5 * 1024 * 1024 + 1, NOW))
                .hasMessage("La imagen no puede superar 5 MB");
        assertThatThrownBy(() -> MediaFile.register(customer(), "image/jpeg", 0, NOW))
                .isInstanceOf(ValidationException.class);
    }
}
