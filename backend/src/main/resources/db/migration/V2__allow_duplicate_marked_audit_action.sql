ALTER TABLE audit_logs
    DROP CONSTRAINT audit_logs_action_type_check;

ALTER TABLE audit_logs
    ADD CONSTRAINT audit_logs_action_type_check CHECK (
        action_type IN (
            'TICKET_CREATED',
            'STATUS_CHANGED',
            'TRIAGE_REQUESTED',
            'AI_TRIAGE_COMPLETED',
            'AI_TRIAGE_FAILED',
            'TRIAGE_APPROVED',
            'TRIAGE_REJECTED',
            'TRIAGE_EDITED',
            'DUPLICATE_CHECKED',
            'DUPLICATE_MARKED'
        )
    );
