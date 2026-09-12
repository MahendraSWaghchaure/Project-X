-- module-audit owns Flyway version range V80-V89.
-- Correlates an audit row back to the exact HTTP request that caused it - see
-- com.projectx.common.trace.TraceIdFilter, which stamps every request with this same id.
ALTER TABLE audit_log_entries ADD COLUMN trace_id VARCHAR(64);

CREATE INDEX idx_audit_log_entries_trace_id ON audit_log_entries (trace_id);
