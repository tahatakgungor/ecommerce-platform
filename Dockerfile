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

# Maven'ın oluşturduğu JAR dosyasını ismine bakmaksızın yakalayıp app.jar olarak kopyala
# Joker karakter (*) kullanımı burada hayat kurtarır
COPY --from=build /app/product-service/target/*.jar app.jar

# Uygulama portu (Railway'de genelde 8080 beklenir ama 8081 kullanıyorsan kalsın)
EXPOSE 8081

# Uygulamayı başlat
ENTRYPOINT ["java", "-jar", "app.jar"]