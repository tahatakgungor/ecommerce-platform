# 1. Aşama: Maven ile Projeyi Derle (Build Stage)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Önce üstteki ve alttaki pom dosyalarını kopyala (Cache için iyidir)
COPY pom.xml .
COPY product-service/pom.xml product-service/

# Bağımlılıkları indir (Bu aşama bir kez yapılır, sonra hızlanır)
RUN mvn dependency:go-offline -pl product-service -am

# Tüm kodu kopyala ve derle
COPY . .
RUN mvn clean package -DskipTests -pl product-service -am

# 2. Aşama: Sadece Çalıştırılabilir Dosyayı Al (Run Stage)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Derleme aşamasında oluşan jar'ı buraya kopyala
COPY --from=build /app/product-service/target/app.jar app.jar

# Uygulamayı başlat
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]