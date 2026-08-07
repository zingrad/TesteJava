CREATE TABLE currency (
    code        CHAR(3)      NOT NULL,
    name        VARCHAR(60)  NOT NULL,
    minor_unit  SMALLINT     NOT NULL DEFAULT 2,
    CONSTRAINT pk_currency PRIMARY KEY (code),
    CONSTRAINT ck_currency_minor_unit CHECK (minor_unit BETWEEN 0 AND 4)
);

CREATE TABLE receivable_type (
    code           VARCHAR(40)   NOT NULL,
    name           VARCHAR(80)   NOT NULL,
    monthly_spread NUMERIC(9, 6) NOT NULL,
    active         BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_receivable_type PRIMARY KEY (code),
    CONSTRAINT ck_receivable_type_spread CHECK (monthly_spread >= 0)
);

CREATE TABLE assignor (
    id         BIGSERIAL    NOT NULL,
    tax_id     VARCHAR(14)  NOT NULL,
    legal_name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_assignor PRIMARY KEY (id),
    CONSTRAINT uq_assignor_tax_id UNIQUE (tax_id)
);

CREATE TABLE exchange_rate (
    id             BIGSERIAL      NOT NULL,
    base_currency  CHAR(3)        NOT NULL,
    quote_currency CHAR(3)        NOT NULL,
    rate           NUMERIC(18, 8) NOT NULL,
    effective_at   TIMESTAMPTZ    NOT NULL,
    source         VARCHAR(40)    NOT NULL,
    CONSTRAINT pk_exchange_rate PRIMARY KEY (id),
    CONSTRAINT fk_exchange_rate_base FOREIGN KEY (base_currency) REFERENCES currency (code),
    CONSTRAINT fk_exchange_rate_quote FOREIGN KEY (quote_currency) REFERENCES currency (code),
    CONSTRAINT uq_exchange_rate_pair_moment UNIQUE (base_currency, quote_currency, effective_at),
    CONSTRAINT ck_exchange_rate_positive CHECK (rate > 0),
    CONSTRAINT ck_exchange_rate_distinct_pair CHECK (base_currency <> quote_currency)
);

CREATE INDEX ix_exchange_rate_lookup ON exchange_rate (base_currency, quote_currency, effective_at DESC);
