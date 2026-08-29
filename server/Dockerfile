FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY server-all.jar app.jar
COPY data/ /app/data/
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
