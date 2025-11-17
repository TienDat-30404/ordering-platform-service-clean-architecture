# GIAI ĐOẠN 1: BUILDER
FROM maven:3.9-eclipse-temurin-21 AS builder 
WORKDIR /build


COPY pom.xml ./

COPY discovery_eureka ./discovery_eureka

RUN mvn install -N -B 

RUN cd discovery_eureka && \
    mvn clean package -am -DskipTests -B

# ----------------------------------------------------

# GIAI ĐOẠN 2: RUNTIME
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy jar file từ giai đoạn builder
COPY --from=builder /build/discovery_eureka/target/*.jar app.jar

EXPOSE 8761

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8761/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
