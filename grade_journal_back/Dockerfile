from maven:3.9.9-eclipse-temurin-21 as build
workdir /app

copy pom.xml .
copy src ./src

run mvn clean package -DskipTests

from eclipse-temurin:21-jre
workdir /app

copy --from=build /app/target/grade_journal_back-0.0.1-SNAPSHOT.jar app.jar

expose 8080

entrypoint ["java", "-jar", "app.jar"]