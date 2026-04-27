# -- Estagio 1: Build --
# Usa JDK Alpine para manter a imagem de build pequena
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copia o wrapper Maven e baixa dependencias antes do codigo-fonte
# (aproveita cache de camada Docker - as dependencias raramente mudam)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

COPY src src
RUN ./mvnw package -DskipTests -q

# Extrai o JAR em camadas para cache Docker otimizado
# Ordem das camadas: dependencias -> loader -> deps de snapshot -> codigo da aplicacao
RUN java -Djarmode=layertools -jar target/*.jar extract --destination extracted

# -- Estagio 2: Runtime --
# Usa apenas JRE no runtime para imagem menor e menor superficie de ataque
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Cria usuario sem privilegios de root para seguranca (principio do menor privilegio)
RUN addgroup -g 1001 banking && adduser -u 1001 -G banking -s /bin/sh -D banking
USER banking

# Copia camadas do JAR na ordem ideal (camadas mais estaveis primeiro)
COPY --from=builder /workspace/extracted/dependencies/ ./
COPY --from=builder /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/extracted/application/ ./

# Configuracoes da JVM para ambiente containerizado
# UseContainerSupport: respeita limites de memoria do container (cgroups)
# MaxRAMPercentage: usa ate 75% da memoria alocada ao container
# ExitOnOutOfMemoryError: reinicializacao rapida em vez de estado inconsistente
ENV JAVA_OPTS="-XX:+UseContainerSupport \\
               -XX:MaxRAMPercentage=75.0 \\
               -XX:+UseG1GC \\
               -XX:+ExitOnOutOfMemoryError \\
               -Djava.security.egd=file:/dev/./urandom \\
               -Dspring.profiles.active=prod"

EXPOSE 8080

# Verificacao de saude via endpoint de liveness do Actuator
# start-period=60s: aguarda inicializacao completa antes de contar falhas
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
