-- Auditoria de integracao, DB-02: familia nao se apaga, se inativa (V12).
-- Com ON DELETE CASCADE, apagar uma comunidade levaria junto as familias dela
-- (e, em cascata, pessoas e fontes de renda). RESTRICT faz o banco recusar a
-- exclusao de comunidade que ainda tem familia.
ALTER TABLE familia DROP CONSTRAINT fk_familia_comunidade;
ALTER TABLE familia
    ADD CONSTRAINT fk_familia_comunidade
        FOREIGN KEY (comunidade_id)
        REFERENCES comunidade(id)
        ON DELETE RESTRICT;
