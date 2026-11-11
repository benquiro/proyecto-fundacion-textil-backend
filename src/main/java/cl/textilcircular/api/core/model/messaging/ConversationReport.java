package cl.textilcircular.api.core.model.messaging;

import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.error.ConflictException;
import cl.textilcircular.api.core.error.ForbiddenException;
import cl.textilcircular.api.core.error.ValidationException;
import cl.textilcircular.api.core.persistence.BaseEntity;
import cl.textilcircular.api.core.validation.Validators;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/**
 * A participant flags a conversation (optionally one message) for the administrators. Only the
 * reported message is shared with them, as the app promises.
 */
@Getter
@Entity
@Table(name = "conversation_report")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConversationReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "conversation_report_seq")
    @SequenceGenerator(name = "conversation_report_seq", sequenceName = "conversation_report_seq", allocationSize = 50)
    @Column(name = "report_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false, updatable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false, updatable = false)
    private UserAccount reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", updatable = false)
    private Message message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private ReportReason reason;

    @Column(length = Validators.COMMENT_MAX, updatable = false)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReportStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private UserAccount resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_note", length = Validators.COMMENT_MAX)
    private String resolutionNote;

    /** Callers reject a second open report first ({@code existsByConversationIdAndReporterIdAndStatus}). */
    public static ConversationReport file(Conversation conversation, UserAccount reporter, Message message,
                                          ReportReason reason, String detail, Instant now) {
        reporter.ensureActive();
        conversation.requireParticipant(reporter);
        if (message != null && !sameEntity(message.getConversation(), conversation)) {
            throw new ValidationException("messageId", "El mensaje no pertenece a esta conversación");
        }
        ConversationReport report = new ConversationReport();
        report.conversation = conversation;
        report.reporter = reporter;
        report.message = message;
        report.reason = Validators.required("reason", reason, "Elige un motivo");
        report.detail = Validators.optionalText("detail", detail, Validators.COMMENT_MAX, "el detalle");
        report.status = ReportStatus.OPEN;
        report.createdAt = now;
        return report;
    }

    public void resolve(UserAccount administrator, String note, Instant now) {
        close(ReportStatus.RESOLVED, administrator, note, now);
    }

    public void dismiss(UserAccount administrator, String note, Instant now) {
        close(ReportStatus.DISMISSED, administrator, note, now);
    }

    private void close(ReportStatus outcome, UserAccount administrator, String note, Instant now) {
        administrator.ensureActive();
        if (!administrator.isAdministrator()) {
            throw new ForbiddenException();
        }
        if (status != ReportStatus.OPEN) {
            throw new ConflictException("REPORT_CLOSED", "Este reporte ya fue atendido");
        }
        this.resolutionNote = Validators.optionalText("note", note, Validators.COMMENT_MAX, "la nota");
        this.status = outcome;
        this.resolvedBy = administrator;
        this.resolvedAt = now;
    }
}
