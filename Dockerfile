# Imagem da API. Não depende de plataforma: o Render constrói a partir deste
# arquivo, e qualquer máquina com Docker roda a mesma imagem
# (docker build -t cadastro-api . && docker run -p 3333:3333 --env-file .env cadastro-api).
# Passo a passo do deploy em docs/DEPLOY.md.

# ---------------------------------------------------------------------------
# 1. Build: Maven + JDK. Nada deste estágio vai para a imagem final.
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Só o pom primeiro: enquanto ele não mudar, o Docker reaproveita a camada com
# as dependências baixadas e o build seguinte não baixa tudo de novo.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Sem testes aqui: o CI (mvn verify) já roda em todo PR, e a main só recebe
# merge com CI verde. Repetir no deploy dobraria o tempo de build.
RUN mvn -B package -DskipTests \
 && cp target/cadastro-familias-api-*.jar /build/app.jar

# ---------------------------------------------------------------------------
# 2. Runtime: só a JRE 21 e o jar. Sem Maven, sem JDK, sem código-fonte.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

# Usuário sem privilégio: se a aplicação for comprometida, o invasor não é
# root dentro do container.
RUN addgroup -S app && adduser -S -G app -H -s /sbin/nologin app
WORKDIR /app
COPY --from=build --chown=app:app /build/app.jar app.jar
USER app

# A plataforma injeta PORT (o Render usa 10000); a aplicação lê em
# server.port=${PORT:3333}. EXPOSE é só documentação.
ENV PORT=3333
EXPOSE 3333

# Flags para caber em 512 MB com 1 vCPU ou menos (Render gratuito: 0,1 CPU):
#
#  -XX:MaxRAMPercentage=50
#      Heap de no máximo 50% da memória do container (256 MB em 512 MB). O
#      resto é da memória fora do heap, que no Spring Boot com Hibernate é
#      grande: medido, ~190 MB (metaspace 83, símbolos 36, code cache 18...).
#      Com 50%, o pico medido foi 481 MB com cadastro, relatórios, .xlsx e 3
#      logins ao mesmo tempo. Com 55% o pico foi 503 MB, colado no limite, e
#      com 75% (padrão de muito tutorial) o container é morto pelo sistema
#      sem nem um OutOfMemoryError no log. Abaixo disso falta heap para o
#      login: cada um usa 64 MiB (argon2id, SegurancaConfig); com 45%, três
#      logins simultâneos já estouram. Conta completa em docs/DEPLOY.md.
#  -XX:+UseSerialGC
#      Coletor de uma thread, o de menor custo de memória e de CPU. Com 1 CPU
#      os coletores paralelos (G1, Parallel) só disputam o mesmo núcleo com a
#      aplicação, e o heap pequeno é coletado rápido mesmo assim.
#  -XX:TieredStopAtLevel=1
#      Só o compilador JIT C1. Sobe bem mais rápido e gasta menos code cache e
#      CPU compilando; o pico de desempenho do C2 não faz falta para uma
#      usuária. Com 0,1 CPU, isso encurta o "acordar" depois da hibernação.
#  -Xss512k
#      Pilha de 512 KB por thread em vez de 1 MB. Suficiente para o Spring;
#      com até 20 threads do Tomcat (application-prod.yml) mais as da JVM,
#      economiza dezenas de MB no pior caso.
#  -XX:MaxMetaspaceSize=160m
#      Teto para as classes carregadas. Sem teto, um vazamento de classes
#      cresceria até o container morrer sem explicação; com ele, a falha vem
#      com nome (OutOfMemoryError: Metaspace).
#  -XX:+ExitOnOutOfMemoryError
#      Se o heap estourar, a JVM sai na hora. A plataforma reinicia o
#      container e o log mostra o motivo, em vez de a API ficar meio viva,
#      respondendo erro a tudo.
#
# JAVA_TOOL_OPTIONS na plataforma ACRESCENTA flags, mas não troca as daqui: a
# linha de comando vence (testado). Para mudar um destes valores, mude este
# arquivo.
#
# Forma "exec" (lista JSON): o java é o processo 1 e recebe o SIGTERM do
# deploy, o que permite o desligamento gracioso (server.shutdown=graceful).
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=50", \
  "-XX:+UseSerialGC", \
  "-XX:TieredStopAtLevel=1", \
  "-Xss512k", \
  "-XX:MaxMetaspaceSize=160m", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "app.jar"]
