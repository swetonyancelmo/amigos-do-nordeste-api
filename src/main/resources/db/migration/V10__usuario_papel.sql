-- Papel do usuario do painel. So existe ADMIN por enquanto: a agente de campo
-- nao e usuario — e um aparelho autenticado por token (tabela agente, V8).
ALTER TABLE usuario ADD COLUMN papel varchar(20) NOT NULL DEFAULT 'ADMIN';
