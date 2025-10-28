# GIAI ĐOẠN 1: BUILDER
# Sử dụng image đã cài sẵn Maven và JDK để tối ưu tốc độ build và tránh lỗi 'mvn: not found'.
FROM maven:3.9-eclipse-temurin-21 AS builder 
WORKDIR /build

# 1. Copy parent pom (BẮT BUỘC)
# Phải copy Parent POM để Maven biết cấu trúc Multi-Module
COPY pom.xml ./

# 2. COPY TOÀN BỘ CÁC THƯ MỤC MODULE
# Việc copy toàn bộ thư mục (kể cả src và pom.xml) là cần thiết để Maven 
# thiết lập "Reactor" (danh sách các module) một cách chính xác.
# CHÚ Ý: Cần file .dockerignore để loại trừ thư mục 'target' và các file lớn khác.
COPY discovery_eureka ./discovery_eureka

# 3. Quét Modules và Tải Dependencies
# Chạy 'mvn install -N' để buộc Maven quét và phân giải tất cả các module trong Reactor.
RUN mvn install -N -B 

# 4. CHUYỂN VÀO THƯ MỤC MODULE CON VÀ BUILD
# BƯỚC 4.1: Chuyển thư mục làm việc vào module con
RUN cd discovery_eureka && \
    # BƯỚC 4.2: Build module (không cần dùng cờ -pl)
    mvn clean package -am -DskipTests -B

# ----------------------------------------------------

# GIAI ĐOẠN 2: RUNTIME
# Sử dụng image JRE nhẹ hơn cho môi trường chạy
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy jar file từ giai đoạn builder
# LƯU Ý: Đường dẫn file JAR vẫn là /build/discovery_eureka/target/*.jar
COPY --from=builder /build/discovery_eureka/target/*.jar app.jar

EXPOSE 8761

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8761/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
