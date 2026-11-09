package cl.textilcircular.api.core.repository;

import cl.textilcircular.api.core.model.messaging.Conversation;
import cl.textilcircular.api.core.model.messaging.Message;
import cl.textilcircular.api.core.model.account.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Optional<Message> findByClientUuid(UUID clientUuid);

    List<Message> findByConversationIdOrderBySentAtAscIdAsc(Long conversationId);

    /** Marks as read what the other participant sent; refuses readers outside the conversation. */
    default int markReadBy(Conversation conversation, UserAccount reader, Instant now) {
        conversation.requireParticipant(reader);
        return markUnreadFromOthers(conversation.getId(), reader.getId(), now);
    }

    /**
     * Low-level update behind {@link #markReadBy}; does not check participation. {@code read_at}
     * is never earlier than {@code sent_at}, even if the sender's clock was ahead.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Message m set m.readAt = greatest(m.sentAt, :now)
            where m.conversation.id = :conversationId and m.sender.id <> :readerId and m.readAt is null
            """)
    int markUnreadFromOthers(Long conversationId, Long readerId, Instant now);

    /** Total unread messages for the navigation badge. */
    @Query("""
            select count(m) from Message m join m.conversation c join c.business b
            where m.readAt is null and m.sender.id <> :userId
              and (c.initiator.id = :userId or b.owner.id = :userId)
            """)
    long countUnreadFor(Long userId);
}
