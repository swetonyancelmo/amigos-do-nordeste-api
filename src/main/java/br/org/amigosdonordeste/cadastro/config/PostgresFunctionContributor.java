package br.org.amigosdonordeste.cadastro.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.SqlTypes;

public class PostgresFunctionContributor implements FunctionContributor {
  @Override
  public void contributeFunctions(FunctionContributions functionContributions) {
    BasicTypeReference<String> stringType = new BasicTypeReference<>("string", String.class, SqlTypes.VARCHAR);
    functionContributions.getFunctionRegistry().registerNamed(
      "unaccent",
      functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(stringType)
    );
  }
}
