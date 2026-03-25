# 1. Aşama: Build Stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Tüm projeyi kopyala (Multi-module olduğu için root'taki pom.xml de lazım)
COPY . .

# Product-service modülünü derle ve testleri atla
# -pl ile sadece ilgili modülü, -am ile bağımlı olduğu üst modülleri dahil ediyoruz
RUN mvn clean package -DskipTests -pl product-service -am

# 2. Aşama: Run Stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Dosyayı build aşamasından alıp direkt /app içine kopyalıyoruz
COPY --from=build /app/product-service/target/*.jar /app/app.jar

EXPOSE 8081

# Dosyayı tam yoluyla (absolute path) çalıştırıyoruz
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
