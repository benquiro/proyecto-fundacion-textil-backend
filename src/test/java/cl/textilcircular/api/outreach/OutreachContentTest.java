package cl.textilcircular.api.outreach;

import cl.textilcircular.api.account.UserAccount;
import cl.textilcircular.api.shared.error.ConflictException;
import cl.textilcircular.api.shared.error.ForbiddenException;
import cl.textilcircular.api.shared.error.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static cl.textilcircular.api.support.Fixtures.NOW;
import static cl.textilcircular.api.support.Fixtures.administrator;
import static cl.textilcircular.api.support.Fixtures.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutreachContentTest {

    UserAccount admin = administrator();
    Instant eventAt = Instant.parse("2026-12-01T15:00:00Z");

    @Test
    void onlyAdministratorsPublish() {
        assertThatThrownBy(() -> OutreachContent.publish(customer(), ContentType.TEXT, null, "Hola", null, null, null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void eachTypeRequiresItsFields() {
        assertThatThrownBy(() -> OutreachContent.publish(admin, ContentType.TEXT, "T", " ", null, null, null))
                .isInstanceOf(ValidationException.class).extracting("field").isEqualTo("body");
        assertThatThrownBy(() -> OutreachContent.publish(admin, ContentType.IMAGE, null, null, null, null, null))
                .isInstanceOf(ValidationException.class).extracting("field").isEqualTo("imageUrl");
        assertThatThrownBy(() -> OutreachContent.publish(admin, ContentType.ARTICLE, null, "Cuerpo", null, null, null))
                .isInstanceOf(ValidationException.class).extracting("field").isEqualTo("title");
        assertThatThrownBy(() -> OutreachContent.publish(admin, ContentType.EVENT, "Feria", null, null, null, "Plaza"))
                .isInstanceOf(ValidationException.class).extracting("field").isEqualTo("eventAt");
        assertThatThrownBy(() -> OutreachContent.publish(null, ContentType.TEXT, null, "x", null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void imageMustBeAWebOrMediaLink() {
        assertThatThrownBy(() -> OutreachContent.publish(admin, ContentType.IMAGE, null, null, "javascript:alert(1)", null, null))
                .isInstanceOf(ValidationException.class).extracting("field").isEqualTo("imageUrl");
    }

    @Test
    void eventKeepsDateAndPlace() {
        OutreachContent event = OutreachContent.publish(admin, ContentType.EVENT, " Feria textil ", null, null, eventAt, " Plaza Ñuñoa ");

        assertThat(event.getTitle()).isEqualTo("Feria textil");
        assertThat(event.getEventAt()).isEqualTo(eventAt);
        assertThat(event.getEventLocation()).isEqualTo("Plaza Ñuñoa");
        assertThat(event.getAuthor()).isSameAs(admin);
    }

    @Test
    void eventFieldsAreDroppedForOtherTypes() {
        OutreachContent text = OutreachContent.publish(admin, ContentType.TEXT, null, "Noticia", null, eventAt, "Plaza");

        assertThat(text.getEventAt()).isNull();
        assertThat(text.getEventLocation()).isNull();
    }

    @Test
    void administratorUpdatesAndDeletes() {
        OutreachContent content = OutreachContent.publish(admin, ContentType.ARTICLE, "Título", "Cuerpo", null, null, null);

        content.update(administrator(), ContentType.IMAGE, null, null, "https://img/afiche.jpg", null, null);
        assertThat(content.getType()).isEqualTo(ContentType.IMAGE);
        assertThat(content.getImageUrl()).isEqualTo("https://img/afiche.jpg");
        assertThat(content.getTitle()).isNull();

        assertThatThrownBy(() -> content.delete(customer(), NOW)).isInstanceOf(ForbiddenException.class);
        content.delete(admin, NOW);
        assertThat(content.isDeleted()).isTrue();
        assertThatThrownBy(() -> content.update(admin, ContentType.TEXT, null, "x", null, null, null))
                .isInstanceOf(ConflictException.class);
    }
}
