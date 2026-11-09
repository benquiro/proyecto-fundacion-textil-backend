package cl.textilcircular.api.messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByInitiatorIdAndBusinessId(Long initiatorId, Long businessId);

    /**
     * Conversations where the user is the customer or the business owner, with their last
     * message and the messages from the other side still unread. Latest activity first.
     */
    @Query(value = """
            SELECT c.conversation_id AS "conversationId",
                   c.business_id     AS "businessId",
                   b.name            AS "businessName",
                   b.user_id         AS "businessOwnerId",
                   c.initiator_id    AS "initiatorId",
                   u.alias           AS "initiatorAlias",
                   lm.content        AS "lastMessage",
                   lm.sent_at        AS "lastMessageAt",
                   lm.sender_id      AS "lastSenderId",
                   (SELECT count(*) FROM message m
                    WHERE m.conversation_id = c.conversation_id
                      AND m.read_at IS NULL
                      AND m.sender_id <> :userId) AS "unreadCount"
            FROM conversation c
                     JOIN business b ON b.business_id = c.business_id
                     JOIN user_account u ON u.user_id = c.initiator_id
                     LEFT JOIN LATERAL (
                         SELECT m.content, m.sent_at, m.sender_id FROM message m
                         WHERE m.conversation_id = c.conversation_id
                         ORDER BY m.sent_at DESC, m.message_id DESC
                         LIMIT 1) lm ON true
            WHERE c.initiator_id = :userId OR b.user_id = :userId
            ORDER BY coalesce(lm.sent_at, c.created_at) DESC, c.conversation_id DESC
            """, nativeQuery = true)
    List<InboxItem> findInbox(Long userId);
}
