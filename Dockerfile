# ===== Stage 1: 构建 =====
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /src

# 先复制所有 pom.xml（变化频率低，利用缓存）
COPY pom.xml .
COPY share-expense-domain/pom.xml share-expense-domain/
COPY share-expense-infrastructure/pom.xml share-expense-infrastructure/
COPY share-expense-app/pom.xml share-expense-app/
COPY share-expense-adapter/pom.xml share-expense-adapter/
COPY share-expense-client/pom.xml share-expense-client/
COPY share-expense-ai/pom.xml share-expense-ai/
COPY start/pom.xml start/

# 下载依赖（所有模块的依赖一起下完）
RUN mvn dependency:go-offline -DskipTests

# 再复制源码（变化频率高）
COPY share-expense-domain/src/ share-expense-domain/src/
COPY share-expense-infrastructure/src/ share-expense-infrastructure/src/
COPY share-expense-app/src/ share-expense-app/src/
COPY share-expense-adapter/src/ share-expense-adapter/src/
COPY share-expense-client/src/ share-expense-client/src/
COPY share-expense-ai/src/ share-expense-ai/src/
COPY start/src/ start/src/

# 构建
RUN mvn package -DskipTests

# ===== Stage 2: 运行 =====
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# start 模块是 Spring Boot 入口，它的 target 里有可执行 jar
COPY --from=builder /src/start/target/*.jar app.jar

EXPOSE 8081
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
