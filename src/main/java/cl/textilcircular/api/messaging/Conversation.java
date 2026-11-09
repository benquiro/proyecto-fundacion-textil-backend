package cl.textilcircular.api.messaging;

import cl.textilcircular.api.account.UserAccount;
import cl.textilcircular.api.business.Business;
import cl.textilcircular.api.product.Product;
import cl.textilcircular.api.shared.error.ForbiddenException;
import cl.textilcircular.api.shared.error.NotFoundException;
import cl.textilcircular.api.shared.error.ValidationException;
import cl.textilcircular.api.shared.persistence.BaseEntity;
import cl.textilcircular.api.shared.validation.Validators;
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

/**
 * Chat between a customer and a business. The participants are the customer who started it and
 * the business owner; nobody else can read, send or mark messages.
 */
@Getter
@Entity
@Table(name = "conversation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversation extends BaseEntity {

    private static final String CUSTOMER_FALLBACK_NAME = "Cliente";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "conversation_seq")
    @SequenceGenerator(name = "conversation_seq", sequenceName = "conversation_seq", allocationSize = 50)
    @Column(name = "conversation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "initiator_id", nullable = false, updatable = false)
    private UserAccount initiator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false, updatable = false)
    private Business business;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * A customer contacts a business of the directory. Callers first look for the existing
     * thread ({@code findByInitiatorIdAndBusinessId}) and reuse it.
     */
    public static Conversation start(UserAccount customer, Business business, Instant now) {
        customer.ensureActive();
        if (!customer.isCustomer()) {
            throw new ForbiddenException("Solo los clientes pueden iniciar una conversación");
        }
        if (!business.isPublic()) {
            throw new NotFoundException();
        }
        Conversation conversation = new Conversation();
        conversation.initiator = customer;
        conversation.business = business;
        conversation.createdAt = now;
        return conversation;
    }

    /**
     * New message from a participant. Idempotency is the caller's job and happens after this
     * participant check: look up {@code clientUuid} and use {@link Message#isRetryBy}.
     */
    public Message send(UserAccount sender, UUID clientUuid, String content, Product product, Instant now) {
        sender.ensureActive();
        requireParticipant(sender);
        UUID uuid = Validators.required("clientUuid", clientUuid, "Falta el identificador del mensaje");
        String text = Validators.text("content", content, Validators.MESSAGE_MAX, "el mensaje");
        if (product != null && !sameEntity(product.getBusiness(), business)) {
            throw new ValidationException("productId", "El producto no pertenece a este taller");
        }
        return new Message(this, sender, uuid, text, product, now);
    }

    public boolean isParticipant(UserAccount account) {
        return sameEntity(initiator, account) || business.isOwnedBy(account);
    }

    public void requireParticipant(UserAccount account) {
        if (!isParticipant(account)) {
            throw new ForbiddenException();
        }
    }

    /** Name of the other side, as the viewer sees it in the inbox. */
    public String counterpartName(UserAccount viewer) {
        return sameEntity(initiator, viewer) ? business.getName() : initiator.displayName(CUSTOMER_FALLBACK_NAME);
    }
}
