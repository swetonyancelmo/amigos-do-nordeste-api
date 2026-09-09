-- Familia: banheiro (varchar) -> tem_banheiro (boolean), para bater com Familia.temBanheiro
ALTER TABLE familia ADD COLUMN tem_banheiro BOOLEAN;
ALTER TABLE familia DROP COLUMN banheiro;

-- Familia: criado_em/atualizado_em devem ser timestamptz, como usuario.criado_em/ultimo_acesso_em (V1);
-- a entidade usa OffsetDateTime. Timestamps existentes são tratados como UTC na conversão.
ALTER TABLE familia ALTER COLUMN criado_em TYPE TIMESTAMPTZ USING criado_em AT TIME ZONE 'UTC';
ALTER TABLE familia ALTER COLUMN atualizado_em TYPE TIMESTAMPTZ USING atualizado_em AT TIME ZONE 'UTC';

-- Pessoa: numero_calcado permanece VARCHAR (entidade usa String numeroCalcado, pois o
-- endpoint recebe valores como "26/27"); nenhuma mudança necessária nessa coluna.

-- Pessoa: chk_pessoa_idade também precisa aceitar os três campos nulos (cadastro totalmente
-- incompleto, RF-09 - ex.: "filha de Jane" sem nome e sem idade nenhuma)
ALTER TABLE pessoa DROP CONSTRAINT chk_pessoa_idade;
ALTER TABLE pessoa ADD CONSTRAINT chk_pessoa_idade CHECK (
    data_nascimento IS NOT NULL
    OR (idade_estimada IS NULL) = (idade_estimada_em IS NULL)
);

-- Tamanhos de varchar fora de sincronia com a entidade JPA
ALTER TABLE comunidade ALTER COLUMN tipo TYPE VARCHAR(30);
ALTER TABLE comunidade ALTER COLUMN lider_telefone TYPE VARCHAR(20);

ALTER TABLE familia ALTER COLUMN responsavel_cpf TYPE VARCHAR(11);
ALTER TABLE familia ALTER COLUMN telefone TYPE VARCHAR(20);
ALTER TABLE familia ALTER COLUMN escoamento_sanitario TYPE VARCHAR(40);
ALTER TABLE familia ALTER COLUMN tratamento_agua TYPE VARCHAR(40);

ALTER TABLE pessoa ALTER COLUMN sexo TYPE VARCHAR(20);
ALTER TABLE pessoa ALTER COLUMN parentesco TYPE VARCHAR(30);
ALTER TABLE pessoa ALTER COLUMN serie TYPE VARCHAR(40);

ALTER TABLE fonte_renda ALTER COLUMN tipo TYPE VARCHAR(30);
ALTER TABLE fonte_renda ALTER COLUMN faixa TYPE VARCHAR(30);
