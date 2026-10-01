-- A faixa de renda passa a ser da família, não de cada fonte (ADR-0003,
-- revisão de 01/10/2026). Faixa por fonte não se soma: "até 1 salário" +
-- "até 1 salário" pode ser qualquer coisa entre 0 e 2, e o sistema nunca
-- conseguia dizer quanto entra na casa. A fonte fica só com tipo e dono.
ALTER TABLE familia ADD COLUMN faixa_renda VARCHAR(30);

-- Só aproveita a faixa antiga onde ela já era a renda da casa inteira: a
-- família com uma fonte só. Com duas ou mais, qualquer escolha seria chute,
-- e a faixa fica em branco para quem revisar preencher.
UPDATE familia f
   SET faixa_renda = fr.faixa
  FROM fonte_renda fr
 WHERE fr.familia_id = f.id
   AND fr.faixa IS NOT NULL
   AND (SELECT count(*) FROM fonte_renda outra WHERE outra.familia_id = f.id) = 1;

-- "Nenhuma" deixa de ser tipo de fonte: sem renda é a faixa SEM_RENDA_FIXA.
UPDATE familia f
   SET faixa_renda = 'SEM_RENDA_FIXA'
 WHERE f.faixa_renda IS NULL
   AND EXISTS (SELECT 1 FROM fonte_renda fr WHERE fr.familia_id = f.id AND fr.tipo = 'NENHUMA')
   AND NOT EXISTS (SELECT 1 FROM fonte_renda fr WHERE fr.familia_id = f.id AND fr.tipo <> 'NENHUMA');

DELETE FROM fonte_renda WHERE tipo = 'NENHUMA';

ALTER TABLE fonte_renda DROP COLUMN faixa;
