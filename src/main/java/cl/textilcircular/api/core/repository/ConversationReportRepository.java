package cl.textilcircular.api.core.repository;

import cl.textilcircular.api.core.model.messaging.ConversationReport;
import cl.textilcircular.api.core.model.messaging.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationReportRepository extends JpaRepository<ConversationReport, Long> {

    boolean existsByConversationIdAndReporterIdAndStatus(Long conversationId, Long reporterId, ReportStatus status);

    /** Administrator queue, oldest first. */
    Page<ConversationReport> findByStatusOrderByCreatedAtAsc(ReportStatus status, Pageable pageable);
}
