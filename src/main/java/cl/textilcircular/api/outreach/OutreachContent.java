package cl.textilcircular.api.outreach;

import cl.textilcircular.api.account.UserAccount;
import cl.textilcircular.api.shared.error.ConflictException;
import cl.textilcircular.api.shared.error.ForbiddenException;
import cl.textilcircular.api.shared.persistence.AuditedEntity;
import cl.textilcircular.api.shared.validation.Validators;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

/** Foundation news, posters, articles and events shown on the home feed. Managed by administrators. */
@Getter
@Entity
@Table(name = "outreach_content")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutreachContent extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "outreach_content_seq")
    @SequenceGenerator(name = "outreach_content_seq", sequenceName = "outreach_content_seq", allocationSize = 50)
    @Column(name = "content_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false, updatable = false)
    private UserAccount author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ContentType type;

    @Column(length = Validators.CONTENT_TITLE_MAX)
    private String title;

    @Column(length = Validators.CONTENT_BODY_MAX)
    private String body;

    @Column(name = "image_url", length = Validators.URL_MAX)
    private String imageUrl;

    @Column(name = "event_at")
    private Instant eventAt;

    @Column(name = "event_location", length = Validators.EVENT_LOCATION_MAX)
    private String eventLocation;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private long version;

    public static OutreachContent publish(UserAccount author, ContentType type, String title, String body,
                                          String imageUrl, Instant eventAt, String eventLocation) {
        requireAdministrator(Objects.requireNonNull(author));
        OutreachContent content = new OutreachContent();
        content.author = author;
        content.apply(type, title, body, imageUrl, eventAt, eventLocation);
        return content;
    }

    public void update(UserAccount editor, ContentType type, String title, String body, String imageUrl,
                       Instant eventAt, String eventLocation) {
        requireAdministrator(editor);
        requireNotDeleted();
        apply(type, title, body, imageUrl, eventAt, eventLocation);
    }

    public void delete(UserAccount editor, Instant now) {
        requireAdministrator(editor);
        requireNotDeleted();
        deletedAt = now;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** Required fields depend on the type; event date and place are dropped for non-events. */
    private void apply(ContentType type, String title, String body, String imageUrl, Instant eventAt,
                       String eventLocation) {
        ContentType validType = Validators.required("type", type, "Elige el tipo de contenido");
        boolean needsTitle = validType == ContentType.ARTICLE || validType == ContentType.EVENT;
        boolean needsBody = validType == ContentType.TEXT || validType == ContentType.ARTICLE;
        boolean isEvent = validType == ContentType.EVENT;

        String validTitle = needsTitle
                ? Validators.text("title", title, Validators.CONTENT_TITLE_MAX, "el título")
                : Validators.optionalText("title", title, Validators.CONTENT_TITLE_MAX, "el título");
        String validBody = needsBody
                ? Validators.text("body", body, Validators.CONTENT_BODY_MAX, "el texto")
                : Validators.optionalText("body", body, Validators.CONTENT_BODY_MAX, "el texto");
        String validImage = validType == ContentType.IMAGE
                ? Validators.text("imageUrl", imageUrl, Validators.URL_MAX, "la imagen")
                : Validators.optionalText("imageUrl", imageUrl, Validators.URL_MAX, "la imagen");
        Instant validEventAt = isEvent ? Validators.required("eventAt", eventAt, "Elige la fecha del evento") : null;
        String validLocation = isEvent
                ? Validators.optionalText("eventLocation", eventLocation, Validators.EVENT_LOCATION_MAX, "el lugar")
                : null;

        this.type = validType;
        this.title = validTitle;
        this.body = validBody;
        this.imageUrl = validImage;
        this.eventAt = validEventAt;
        this.eventLocation = validLocation;
    }

    private void requireNotDeleted() {
        if (isDeleted()) {
            throw new ConflictException("CONTENT_DELETED", "Este contenido fue eliminado");
        }
    }

    private static void requireAdministrator(UserAccount account) {
        account.ensureActive();
        if (!account.isAdministrator()) {
            throw new ForbiddenException();
        }
    }
}
