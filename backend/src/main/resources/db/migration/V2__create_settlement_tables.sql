CREATE TABLE settlement (
    id                  BIGSERIAL      NOT NULL,
    reference           VARCHAR(40)    NOT NULL,
    assignor_id         BIGINT         NOT NULL,
    face_currency       CHAR(3)        NOT NULL,
    payment_currency    CHAR(3)        NOT NULL,
    base_monthly_rate   NUMERIC(9, 6)  NOT NULL,
    exchange_rate_id    BIGINT,
    exchange_rate_value NUMERIC(18, 8),
    total_face_value    NUMERIC(18, 2) NOT NULL,
    total_present_value NUMERIC(18, 2) NOT NULL,
    total_net_amount    NUMERIC(18, 2) NOT NULL,
    status              VARCHAR(20)    NOT NULL,
    requested_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    settled_at          TIMESTAMPTZ,
    version             BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_settlement PRIMARY KEY (id),
    CONSTRAINT uq_settlement_reference UNIQUE (reference),
    CONSTRAINT fk_settlement_assignor FOREIGN KEY (assignor_id) REFERENCES assignor (id),
    CONSTRAINT fk_settlement_face_currency FOREIGN KEY (face_currency) REFERENCES currency (code),
    CONSTRAINT fk_settlement_payment_currency FOREIGN KEY (payment_currency) REFERENCES currency (code),
    CONSTRAINT fk_settlement_exchange_rate FOREIGN KEY (exchange_rate_id) REFERENCES exchange_rate (id),
    CONSTRAINT ck_settlement_status CHECK (status IN ('PENDING', 'SETTLED', 'CANCELLED')),
    CONSTRAINT ck_settlement_amounts CHECK (
        total_face_value > 0 AND total_present_value > 0 AND total_net_amount > 0
    ),
    CONSTRAINT ck_settlement_cross_currency_rate CHECK (
        (face_currency = payment_currency AND exchange_rate_id IS NULL)
        OR (face_currency <> payment_currency AND exchange_rate_id IS NOT NULL AND exchange_rate_value IS NOT NULL)
    ),
    CONSTRAINT ck_settlement_settled_at CHECK (
        (status = 'SETTLED' AND settled_at IS NOT NULL) OR (status <> 'SETTLED' AND settled_at IS NULL)
    )
);

CREATE TABLE settlement_item (
    id                   BIGSERIAL      NOT NULL,
    settlement_id        BIGINT         NOT NULL,
    receivable_type_code VARCHAR(40)    NOT NULL,
    document_number      VARCHAR(40)    NOT NULL,
    face_value           NUMERIC(18, 2) NOT NULL,
    issue_date           DATE           NOT NULL,
    due_date             DATE           NOT NULL,
    term_days            INTEGER        NOT NULL,
    applied_spread       NUMERIC(9, 6)  NOT NULL,
    present_value        NUMERIC(18, 2) NOT NULL,
    net_amount           NUMERIC(18, 2) NOT NULL,
    CONSTRAINT pk_settlement_item PRIMARY KEY (id),
    CONSTRAINT fk_settlement_item_settlement FOREIGN KEY (settlement_id)
        REFERENCES settlement (id) ON DELETE CASCADE,
    CONSTRAINT fk_settlement_item_type FOREIGN KEY (receivable_type_code)
        REFERENCES receivable_type (code),
    CONSTRAINT uq_settlement_item_document UNIQUE (settlement_id, document_number),
    CONSTRAINT ck_settlement_item_face_value CHECK (face_value > 0),
    CONSTRAINT ck_settlement_item_term CHECK (term_days > 0),
    CONSTRAINT ck_settlement_item_dates CHECK (due_date > issue_date)
);

CREATE INDEX ix_settlement_settled_at ON settlement (settled_at DESC);
CREATE INDEX ix_settlement_assignor_settled_at ON settlement (assignor_id, settled_at DESC);
CREATE INDEX ix_settlement_payment_currency_settled_at ON settlement (payment_currency, settled_at DESC);
CREATE INDEX ix_settlement_item_settlement ON settlement_item (settlement_id);
