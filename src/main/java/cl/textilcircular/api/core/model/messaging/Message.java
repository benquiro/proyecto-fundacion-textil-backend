package cl.textilcircular.api.core.model.messaging;

import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.model.product.Product;
import cl.textilcircular.api.core.error.ConflictException;
import cl.textilcircular.api.core.persistence.BaseEntity;
import cl.textilcircular.api.core.validation.Validators;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Text message of a {@link Conversation}; created only by {@link Conversation#send}. */
@Getter
@Entity
@Table(name = "message")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "message_seq")
    @SequenceGenerator(name = "message_seq", sequenceName = "message_seq", allocationSize = 50)
    @Column(name = "message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false, updatable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false, updatable = false)
    private UserAccount sender;

    @Column(name = "client_uuid", nullable = false, updatable = false)
    private UUID clientUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", updatable = false)
    private Product product;

    @Column(nullable = false, length = Validators.MESSAGE_MAX, updatable = false)
    private String content;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    @Column(name = "read_at")
    private Instant readAt;

    Message(Conversation conversation, UserAccount sender, UUID clientUuid, String content, Product product, Instant now) {
        this.conversation = conversation;
        this.sender = sender;
        this.clientUuid = clientUuid;
        this.content = content;
        this.product = product;
        this.sentAt = now;
    }

    /**
     * {@code true} when a send with this message's uuid is a retry of it. The same uuid from
     * another sender or conversation is a conflict, never a silent success.
     */
    public boolean isRetryBy(Conversation conversation, UserAccount sender) {
        if (sameEntity(this.conversation, conversation) && sameEntity(this.sender, sender)) {
            return true;
        }
        throw new ConflictException("MESSAGE_ID_CONFLICT", "Ese identificador de mensaje ya fue usado");
    }
}
