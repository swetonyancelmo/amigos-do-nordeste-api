-- Numero de comodos do domicilio, para a sentinela "relacao morador/comodo"
-- da Escala de Coelho-Savassi (ADR-0010). Vale "familia = domicilio" da
-- ADR-0003. Nulo de proposito: nenhum cadastro antigo tem esse dado, e
-- nulo e "nao sei" — nunca "zero comodos". O total de moradores NAO vira
-- coluna: e a contagem de pessoas (regra 2).
ALTER TABLE familia ADD COLUMN numero_comodos INTEGER;
ALTER TABLE familia
    ADD CONSTRAINT chk_familia_numero_comodos CHECK (numero_comodos IS NULL OR numero_comodos > 0);
