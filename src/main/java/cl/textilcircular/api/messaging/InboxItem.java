package cl.textilcircular.api.messaging;

import java.time.Instant;

/** One row of a user's inbox. The API picks the counterpart name from it depending on the viewer. */
public interface InboxItem {

    Long getConversationId();

    Long getBusinessId();

    String getBusinessName();

    Long getBusinessOwnerId();

    Long getInitiatorId();

    String getInitiatorAlias();

    /** {@code null} for a conversation without messages yet. */
    String getLastMessage();

    Instant getLastMessageAt();

    Long getLastSenderId();

    long getUnreadCount();
}
