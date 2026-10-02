-- Issue #43: familia nao se apaga, se inativa. Apagar levaria junto pessoas,
-- fontes de renda e o historico de contagem (ON DELETE CASCADE). Toda familia
-- que ja existe continua ativa.
ALTER TABLE familia ADD COLUMN ativa BOOLEAN NOT NULL DEFAULT TRUE;
