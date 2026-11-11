package cl.textilcircular.api.core.repository;

import cl.textilcircular.api.core.model.media.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MediaFileRepository extends JpaRepository<MediaFile, UUID> {
}
