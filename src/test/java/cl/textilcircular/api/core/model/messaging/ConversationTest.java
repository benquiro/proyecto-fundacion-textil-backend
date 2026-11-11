package cl.textilcircular.api.core.model.messaging;

import cl.textilcircular.api.core.model.account.Entrepreneur;
import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.model.business.Business;
import cl.textilcircular.api.core.model.product.Product;
import cl.textilcircular.api.core.model.product.ProductAttributes;
import cl.textilcircular.api.core.error.ConflictException;
import cl.textilcircular.api.core.error.ForbiddenException;
import cl.textilcircular.api.core.error.NotFoundException;
import cl.textilcircular.api.core.error.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static cl.textilcircular.api.support.Fixtures.NOW;
import static cl.textilcircular.api.support.Fixtures.administrator;
import static cl.textilcircular.api.support.Fixtures.commune;
import static cl.textilcircular.api.support.Fixtures.customer;
import static cl.textilcircular.api.support.Fixtures.entrepreneur;
import static cl.textilcircular.api.support.Fixtures.publicBusiness;
import static cl.textilcircular.api.support.Fixtures.service;
import static cl.textilcircular.api.support.Fixtures.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversationTest {

    Entrepreneur owner = entrepreneur();
    Business business = publicBusiness(owner);
    UserAccount client = customer();

    @Test
    void customerStartsAConversationWithAPublicBusiness() {
        Conversation conversation = Conversation.start(client, business, NOW);

        assertThat(conversation.getInitiator()).isSameAs(client);
        assertThat(conversation.isParticipant(client)).isTrue();
        assertThat(conversation.isParticipant(owner.getAccount())).isTrue();
        assertThat(conversation.isParticipant(customer())).isFalse();
    }

    @Test
    void onlyCustomersStartConversations() {
        assertThatThrownBy(() -> Conversation.start(owner.getAccount(), business, NOW))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> Conversation.start(administrator(), business, NOW))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void nonPublicBusinessCannotBeContacted() {
        Business pending = Business.register(entrepreneur(), "T", "D", null, commune(1L), List.of(service(1L)));

        assertThatThrownBy(() -> Conversation.start(client, pending, NOW)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void participantsSendTrimmedMessages() {
        Conversation conversation = Conversation.start(client, business, NOW);
        UUID uuid = UUID.randomUUID();

        Message message = conversation.send(client, uuid, "  Hola  ", null, NOW);

        assertThat(message.getContent()).isEqualTo("Hola");
        assertThat(message.getClientUuid()).isEqualTo(uuid);
        assertThat(message.getReadAt()).isNull();
        assertThat(conversation.send(owner.getAccount(), UUID.randomUUID(), "Respuesta", null, NOW).getSender())
                .isSameAs(owner.getAccount());
    }

    @Test
    void outsidersCannotSend() {
        Conversation conversation = Conversation.start(client, business, NOW);

        assertThatThrownBy(() -> conversation.send(customer(), UUID.randomUUID(), "Hola", null, NOW))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void messageNeedsContentAndAClientUuid() {
        Conversation conversation = Conversation.start(client, business, NOW);

        assertThatThrownBy(() -> conversation.send(client, UUID.randomUUID(), " ", null, NOW))
                .isInstanceOf(ValidationException.class).hasMessage("Ingresa el mensaje");
        assertThatThrownBy(() -> conversation.send(client, UUID.randomUUID(), "x".repeat(2001), null, NOW))
                .hasMessage("El mensaje admite hasta 2000 caracteres");
        assertThatThrownBy(() -> conversation.send(client, null, "Hola", null, NOW))
                .isInstanceOf(ValidationException.class).extracting("field").isEqualTo("clientUuid");
    }

    @Test
    void messageMayReferToAProductOfThatBusiness() {
        Conversation conversation = Conversation.start(client, business, NOW);
        Product own = product(business, owner);
        Entrepreneur otherOwner = entrepreneur();
        Product foreign = product(publicBusiness(otherOwner), otherOwner);

        assertThat(conversation.send(client, UUID.randomUUID(), "¿Lo tienes?", own, NOW).getProduct()).isSameAs(own);
        assertThatThrownBy(() -> conversation.send(client, UUID.randomUUID(), "Hola", foreign, NOW))
                .isInstanceOf(ValidationException.class).extracting("field").isEqualTo("productId");
    }

    @Test
    void retriedUuidIsRecognizedOnlyForTheSameSenderAndConversation() {
        Conversation conversation = Conversation.start(client, business, NOW);
        Message sent = conversation.send(client, UUID.randomUUID(), "Hola", null, NOW);

        assertThat(sent.isRetryBy(conversation, client)).isTrue();
        assertThatThrownBy(() -> sent.isRetryBy(conversation, owner.getAccount()))
                .isInstanceOf(ConflictException.class).extracting("code").isEqualTo("MESSAGE_ID_CONFLICT");
    }

    @Test
    void counterpartIsTheBusinessForTheClientAndTheClientForTheOwner() {
        Conversation conversation = Conversation.start(client, business, NOW);

        assertThat(conversation.counterpartName(client)).isEqualTo(business.getName());
        assertThat(conversation.counterpartName(owner.getAccount())).isEqualTo("Cliente");
    }

    @Test
    void readingRequiresBeingAParticipant() {
        Conversation conversation = Conversation.start(client, business, NOW);

        conversation.requireParticipant(owner.getAccount());
        assertThatThrownBy(() -> conversation.requireParticipant(customer())).isInstanceOf(ForbiddenException.class);
    }

    private static final java.util.concurrent.atomic.AtomicLong PRODUCT_IDS = new java.util.concurrent.atomic.AtomicLong();

    private static Product product(Business business, Entrepreneur owner) {
        return withId(Product.create(business, owner.getAccount(), "Bolso", "Desc", null,
                List.of("https://img/1.jpg"), ProductAttributes.NONE, NOW), PRODUCT_IDS.incrementAndGet());
    }
}
