-- Indexes for the foreign keys V6 and V7 left without one, so deleting or checking a referenced
-- row never scans these tables.
--
-- Note on V1: Hibernate's pooled optimizer treats each sequence value as the top of a block of 50,
-- so with START WITH 1000 the first generated id is 951, not 1000. Explicit reference and demo ids
-- must stay below 951.

CREATE INDEX ix_analytics_event_actor ON analytics_event (actor_user_id) WHERE actor_user_id IS NOT NULL;
CREATE INDEX ix_analytics_event_product ON analytics_event (product_id) WHERE product_id IS NOT NULL;
CREATE INDEX ix_analytics_event_region ON analytics_event (region_id) WHERE region_id IS NOT NULL;
CREATE INDEX ix_analytics_event_commune ON analytics_event (commune_id) WHERE commune_id IS NOT NULL;
CREATE INDEX ix_analytics_event_service ON analytics_event (service_id) WHERE service_id IS NOT NULL;
CREATE INDEX ix_conversation_report_resolved_by ON conversation_report (resolved_by) WHERE resolved_by IS NOT NULL;
