CREATE TABLE municipio (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    nome varchar(120) NOT NULL,
    uf varchar(2) NOT NULL,
    codigo_ibge varchar(7) UNIQUE
);