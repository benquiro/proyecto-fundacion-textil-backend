package cl.textilcircular.api.media;

import cl.textilcircular.api.account.UserAccount;
import cl.textilcircular.api.shared.error.ValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Uploaded image of the simulated storage. The id is random (not guessable) and names the file
 * on disk; {@link #publicPath()} is the URL stored in photo fields.
 */
@Getter
@Entity
@Table(name = "media_file")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MediaFile implements Persistable<UUID> {

    public static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    @Id
    @Column(name = "file_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    private UserAccount owner;

    @Column(name = "content_type", nullable = false, length = 20, updatable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;

    @Column(name = "storage_path", nullable = false, length = 300, updatable = false)
    private String storagePath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** The id is assigned here, so Spring Data needs to be told the row is new (insert, no select). */
    @Transient
    private boolean newEntity;

    public static MediaFile register(UserAccount owner, String contentType, long sizeBytes, Instant now) {
        String extension = EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new ValidationException("file", "La imagen debe ser JPG, PNG o WEBP");
        }
        if (sizeBytes <= 0) {
            throw new ValidationException("file", "La imagen está vacía");
        }
        if (sizeBytes > MAX_BYTES) {
            throw new ValidationException("file", "La imagen no puede superar 5 MB");
        }
        MediaFile file = new MediaFile();
        file.id = UUID.randomUUID();
        file.owner = owner;
        file.contentType = contentType;
        file.sizeBytes = sizeBytes;
        file.storagePath = file.id + extension;
        file.createdAt = now;
        file.newEntity = true;
        return file;
    }

    public String publicPath() {
        return "/media/" + storagePath;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markStored() {
        newEntity = false;
    }
}
