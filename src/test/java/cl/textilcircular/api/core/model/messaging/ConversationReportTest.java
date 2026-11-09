package cl.textilcircular.api.core.model.messaging;

import cl.textilcircular.api.core.model.account.Entrepreneur;
import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.error.ConflictException;
import cl.textilcircular.api.core.error.ForbiddenException;
import cl.textilcircular.api.core.error.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static cl.textilcircular.api.support.Fixtures.NOW;
import static cl.textilcircular.api.support.Fixtures.administrator;
import static cl.textilcircular.api.support.Fixtures.customer;
import static cl.textilcircular.api.support.Fixtures.entrepreneur;
import static cl.textilcircular.api.support.Fixtures.publicBusiness;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversationReportTest {

    Entrepreneur owner = entrepreneur();
    UserAccount client = customer();
    Conversation conversation = Conversation.start(client, publicBusiness(owner), NOW);
    Message message = conversation.send(owner.getAccount(), UUID.randomUUID(), "Spam", null, NOW);

    @Test
    void participantReportsAMessageOfTheConversation() {
        ConversationReport report = ConversationReport.file(conversation, client, message, ReportReason.SPAM, " Publicidad ", NOW);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.OPEN);
        assertThat(report.getDetail()).isEqualTo("Publicidad");
        assertThat(report.getMessage()).isSameAs(message);
    }

    @Test
    void outsidersCannotReportAndTheMessageMustBelongToTheConversation() {
        assertThatThrownBy(() -> ConversationReport.file(conversation, customer(), null, ReportReason.SPAM, null, NOW))
                .isInstanceOf(ForbiddenException.class);

        Conversation other = Conversation.start(customer(), publicBusiness(entrepreneur()), NOW);
        Message foreign = other.send(other.getInitiator(), UUID.randomUUID(), "Hola", null, NOW);
        assertThatThrownBy(() -> ConversationReport.file(conversation, client, foreign, ReportReason.SPAM, null, NOW))
                .isInstanceOf(ValidationException.class).extracting("field").isEqualTo("messageId");
    }

    @Test
    void reasonIsRequired() {
        assertThatThrownBy(() -> ConversationReport.file(conversation, client, null, null, null, NOW))
                .isInstanceOf(ValidationException.class).extracting("field").isEqualTo("reason");
    }

    @Test
    void administratorClosesAReportOnce() {
        ConversationReport report = ConversationReport.file(conversation, client, null, ReportReason.HARASSMENT, null, NOW);
        UserAccount admin = administrator();

        assertThatThrownBy(() -> report.resolve(client, "x", NOW)).isInstanceOf(ForbiddenException.class);

        report.resolve(admin, " Se advirtió al taller ", NOW);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(report.getResolvedBy()).isSameAs(admin);
        assertThat(report.getResolutionNote()).isEqualTo("Se advirtió al taller");
        assertThatThrownBy(() -> report.dismiss(admin, null, NOW)).isInstanceOf(ConflictException.class);
    }
}
