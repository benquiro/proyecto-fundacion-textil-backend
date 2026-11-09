package cl.textilcircular.api.messaging;

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

    /**
     * Marks as read the messages the other participant sent. Callers check that the reader is a
     * participant ({@link Conversation#requireParticipant}) before calling.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Message m set m.readAt = :now
            where m.conversation.id = :conversationId and m.sender.id <> :readerId and m.readAt is null
            """)
    int markRead(Long conversationId, Long readerId, Instant now);

    /** Total unread messages for the navigation badge. */
    @Query("""
            select count(m) from Message m join m.conversation c join c.business b
            where m.readAt is null and m.sender.id <> :userId
              and (c.initiator.id = :userId or b.owner.id = :userId)
            """)
    long countUnreadFor(Long userId);
}
