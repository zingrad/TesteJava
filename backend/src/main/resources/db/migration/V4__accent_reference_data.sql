-- A V3 foi escrita sem acentuação e já está aplicada nos ambientes; editá-la quebraria o checksum
-- do Flyway. A correção vem como migração nova, que é o caminho correto de evolução de schema.

UPDATE currency SET name = 'Dólar norte-americano' WHERE code = 'USD';

UPDATE receivable_type SET name = 'Cheque pré-datado' WHERE code = 'POST_DATED_CHECK';

UPDATE assignor SET legal_name = 'Metalúrgica Aurora LTDA' WHERE tax_id = '11222333000181';
