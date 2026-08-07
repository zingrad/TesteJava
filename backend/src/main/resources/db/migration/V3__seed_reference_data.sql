INSERT INTO currency (code, name, minor_unit) VALUES
    ('BRL', 'Real brasileiro', 2),
    ('USD', 'Dolar norte-americano', 2);

INSERT INTO receivable_type (code, name, monthly_spread) VALUES
    ('MERCANTILE_INVOICE', 'Duplicata mercantil', 0.015000),
    ('POST_DATED_CHECK', 'Cheque pre-datado', 0.025000);

INSERT INTO exchange_rate (base_currency, quote_currency, rate, effective_at, source) VALUES
    ('USD', 'BRL', 5.42000000, '2026-01-02 12:00:00+00', 'MANUAL'),
    ('BRL', 'USD', 0.18450000, '2026-01-02 12:00:00+00', 'MANUAL');

INSERT INTO assignor (tax_id, legal_name) VALUES
    ('11222333000181', 'Metalurgica Aurora LTDA'),
    ('44555666000199', 'Distribuidora Horizonte SA');
