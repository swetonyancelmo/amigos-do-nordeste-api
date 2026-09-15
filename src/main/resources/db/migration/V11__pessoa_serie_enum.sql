-- pessoa.serie deixa de ser texto livre e passa a guardar o name() do enum
-- Serie (regra 3: lista fechada). O que já estava gravado fora da lista vira
-- NULL, senão a entidade não carrega. Sem mudança de tipo: VARCHAR(40) cabe.
UPDATE pessoa
   SET serie = NULL
 WHERE serie IS NOT NULL
   AND serie NOT IN ('PRE', 'ANO_1', 'ANO_2', 'ANO_3', 'ANO_4', 'ANO_5',
                     'ANO_6', 'ANO_7', 'ANO_8', 'ANO_9', 'ENSINO_MEDIO', 'NAO_SE_APLICA');
