# GIAI ĐOẠN 1: BUILDER
FROM maven:3.9-eclipse-temurin-21 AS builder 

WORKDIR /build

# 1. COPY file pom.xml gốc
COPY pom.xml ./

COPY payment-service ./payment-service
COPY common-dtos ./common-dtos 

RUN sed -i '/<module>api-gateway<\/module>/d' pom.xml && \
    sed -i '/<module>user-service<\/module>/d' pom.xml && \
    sed -i '/<module>restaurant-service<\/module>/d' pom.xml && \
    sed -i '/<module>order-service<\/module>/d' pom.xml && \
    mvn clean install -DskipTests -B -pl payment-service -am
# ----------------------------------------------------

# GIAI ĐOẠN 2: RUNTIME
FROM maven:3.9-eclipse-temurin-21

WORKDIR /build

# <CHANGE> Copy source code và dependencies từ builder
COPY --from=builder /build /build
COPY --from=builder /root/.m2 /root/.m2

EXPOSE 8081

# <CHANGE> Chạy với mvn spring-boot:run để hỗ trợ hot reload
ENV MAVEN_OPTS="-Dspring-boot.run.fork=false"
WORKDIR /build/payment-service
CMD ["mvn", "spring-boot:run"]
