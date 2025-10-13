# STAGE 1: BUILD với Maven
# Sử dụng Base Image Temurin đã được vá lỗi
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy parent POM
COPY pom.xml /app/

# Xóa module restaurant-service nếu chưa sẵn sàng
RUN sed -i '/<module>order-service<\/module>/d' pom.xml || true

# Copy module POMs
COPY common-dtos/pom.xml /app/common-dtos/
COPY restaurant-service/pom.xml /app/restaurant-service/

# Copy source code
COPY common-dtos/src /app/common-dtos/src
COPY restaurant-service/src /app/restaurant-service/src

# --- BƯỚC SỬA LỖI DEPENDENCY RESOLUTION ---

# 1. BUILD VÀ INSTALL CÁC MODULE PHỤ THUỘC (common-dtos) TRƯỚC
# Lệnh này đảm bảo common-dtos được đóng gói và đặt vào .m2 repository
RUN mvn clean install -pl common-dtos -am -DskipTests

# 2. BUILD MODULE CHÍNH (order-service)
# common-dtos đã có sẵn trong .m2 repo cục bộ, nên lỗi "Could not find artifact" sẽ được giải quyết.
RUN mvn clean install -pl restaurant-service -DskipTests

# ----------------------------------------------------
# STAGE 2: RUNTIME (chỉ JRE)
# ----------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy JAR từ stage builder
COPY --from=builder /app/restaurant-service/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
