package cl.textilcircular.api.core.repository;

import cl.textilcircular.api.core.model.messaging.Conversation;
import cl.textilcircular.api.core.model.messaging.ConversationReport;
import cl.textilcircular.api.core.model.messaging.Message;
import cl.textilcircular.api.core.model.messaging.ReportReason;
import cl.textilcircular.api.core.model.messaging.ReportStatus;
import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.model.business.Business;
import cl.textilcircular.api.core.error.ForbiddenException;
import cl.textilcircular.api.support.PersistedData;
import cl.textilcircular.api.support.RepositoryTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static cl.textilcircular.api.support.Fixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RepositoryTest
class MessagingRepositoriesTest {

    @Autowired ConversationRepository conversations;
    @Autowired MessageRepository messages;
    @Autowired ConversationReportRepository reports;
    @Autowired EntityManager em;
    PersistedData data;
    Business business;
    UserAccount owner;
    UserAccount client;

    @BeforeEach
    void setUp() {
        data = new PersistedData(em);
        business = data.approvedBusiness("Hilos", "Desc", 102L, 1L);
        owner = business.getOwner().getAccount();
        client = data.customer();
    }

    Conversation conversation(UserAccount customer, Business target) {
        return conversations.save(Conversation.start(customer, target, NOW));
    }

    Message send(Conversation conversation, UserAccount sender, String text, Instant at) {
        return messages.save(conversation.send(sender, UUID.randomUUID(), text, null, at));
    }

    @Test
    void conversationPairIsUnique() {
        Conversation first = conversation(client, business);
        em.flush();

        assertThat(conversations.findByInitiatorIdAndBusinessId(client.getId(), business.getId())).contains(first);
        assertThatThrownBy(() -> conversations.saveAndFlush(Conversation.start(client, business, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void messageUuidIsUniqueAndFindable() {
        Conversation conversation = conversation(client, business);
        Message sent = send(conversation, client, "Hola", NOW);
        em.flush();

        assertThat(messages.findByClientUuid(sent.getClientUuid())).contains(sent);
        assertThatThrownBy(() -> messages.saveAndFlush(conversation.send(owner, sent.getClientUuid(), "Otro", null, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void historyIsChronological() {
        Conversation conversation = conversation(client, business);
        send(conversation, client, "1", NOW);
        send(conversation, owner, "2", NOW.plusSeconds(10));
        send(conversation, client, "3", NOW.plusSeconds(20));
        data.flushAndClear();

        assertThat(messages.findByConversationIdOrderBySentAtAscIdAsc(conversation.getId()))
                .extracting(Message::getContent).containsExactly("1", "2", "3");
    }

    @Test
    void markReadOnlyTouchesTheOtherSidesMessages() {
        Conversation conversation = conversation(client, business);
        send(conversation, client, "Hola", NOW);
        send(conversation, owner, "Hola, ¿qué necesitas?", NOW.plusSeconds(5));
        send(conversation, owner, "¿Sigues ahí?", NOW.plusSeconds(6));
        em.flush();

        assertThat(messages.countUnreadFor(client.getId())).isEqualTo(2);
        assertThat(messages.countUnreadFor(owner.getId())).isEqualTo(1);

        int marked = messages.markReadBy(conversation, client, NOW.plusSeconds(60));

        assertThat(marked).isEqualTo(2);
        assertThat(messages.countUnreadFor(client.getId())).isZero();
        assertThat(messages.countUnreadFor(owner.getId())).isEqualTo(1);
    }

    @Test
    void markingReadKeepsLoadedEntitiesManaged() {
        Conversation conversation = conversation(client, business);
        send(conversation, owner, "Hola", NOW);
        em.flush();

        messages.markReadBy(conversation, client, NOW.plusSeconds(60));

        assertThat(em.contains(conversation)).isTrue();
        assertThat(em.contains(client)).isTrue();
    }

    @Test
    void markReadRejectsOutsidersAndToleratesClockSkew() {
        Conversation conversation = conversation(client, business);
        send(conversation, owner, "Enviado por un reloj adelantado", NOW.plusSeconds(30));
        em.flush();

        assertThatThrownBy(() -> messages.markReadBy(conversation, data.customer(), NOW))
                .isInstanceOf(ForbiddenException.class);
        assertThat(messages.markReadBy(conversation, client, NOW)).isEqualTo(1);
        assertThat(messages.countUnreadFor(client.getId())).isZero();
    }

    @Test
    void inboxShowsLastMessageUnreadCountAndNewestFirst() {
        Conversation older = conversation(client, business);
        send(older, client, "Primera", NOW);
        send(older, owner, "Respuesta", NOW.plusSeconds(5));
        Business other = data.approvedBusiness("Retazos", "Desc", 45L, 2L);
        Conversation newer = conversation(client, other);
        send(newer, client, "Hola Retazos", NOW.plusSeconds(100));
        Conversation empty = conversation(data.customer(), business);
        data.flushAndClear();

        List<InboxItem> clientInbox = conversations.findInbox(client.getId());
        assertThat(clientInbox).extracting(InboxItem::getConversationId).containsExactly(newer.getId(), older.getId());
        InboxItem first = clientInbox.getFirst();
        assertThat(first.getBusinessName()).isEqualTo("Retazos");
        assertThat(first.getLastMessage()).isEqualTo("Hola Retazos");
        assertThat(first.getUnreadCount()).isZero();
        assertThat(clientInbox.get(1).getUnreadCount()).isEqualTo(1);
        assertThat(clientInbox.get(1).getLastMessageAt()).isEqualTo(NOW.plusSeconds(5));

        List<InboxItem> ownerInbox = conversations.findInbox(owner.getId());
        assertThat(ownerInbox).extracting(InboxItem::getConversationId).containsExactlyInAnyOrder(older.getId(), empty.getId());
        assertThat(ownerInbox).filteredOn(i -> i.getConversationId().equals(empty.getId()))
                .singleElement().satisfies(i -> assertThat(i.getLastMessage()).isNull());
        assertThat(ownerInbox).filteredOn(i -> i.getConversationId().equals(older.getId()))
                .singleElement().satisfies(i -> {
                    assertThat(i.getInitiatorAlias()).isEqualTo("Cliente");
                    assertThat(i.getUnreadCount()).isEqualTo(1);
                });
    }

    @Test
    void onlyOneOpenReportPerReporterAndConversation() {
        Conversation conversation = conversation(client, business);
        ConversationReport first = reports.saveAndFlush(ConversationReport.file(conversation, client, null, ReportReason.SPAM, null, NOW));
        assertThat(reports.existsByConversationIdAndReporterIdAndStatus(conversation.getId(), client.getId(), ReportStatus.OPEN)).isTrue();
        assertThat(reports.findByStatusOrderByCreatedAtAsc(ReportStatus.OPEN, PageRequest.of(0, 10))).contains(first);

        first.resolve(data.administrator(), null, NOW);
        em.flush(); // Hibernate runs inserts before updates; the resolution must land first
        ConversationReport second = reports.saveAndFlush(ConversationReport.file(conversation, client, null, ReportReason.SCAM, null, NOW));
        assertThat(second.getId()).isNotNull();

        assertThatThrownBy(() -> reports.saveAndFlush(ConversationReport.file(conversation, client, null, ReportReason.OTHER, null, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
