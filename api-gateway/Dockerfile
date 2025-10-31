# GIAI ĐOẠN 1: BUILDER
FROM maven:3.9-eclipse-temurin-21 AS builder 
WORKDIR /build

COPY pom.xml ./

COPY api-gateway ./api-gateway

COPY common-dtos ./common-dtos 

RUN mvn install -N -B 


RUN cd api-gateway && \
    mvn clean package -am -DskipTests -B

# ----------------------------------------------------

# GIAI ĐOẠN 2: RUNTIME
FROM maven:3.9-eclipse-temurin-21

WORKDIR /build

# <CHANGE> Copy source code và dependencies từ builder
COPY --from=builder /build /build
COPY --from=builder /root/.m2 /root/.m2

EXPOSE 8079

# <CHANGE> Chạy với mvn spring-boot:run để hỗ trợ hot reload
ENV MAVEN_OPTS="-Dspring-boot.run.fork=false"
WORKDIR /build/api-gateway
CMD ["mvn", "spring-boot:run"]