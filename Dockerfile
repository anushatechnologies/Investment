FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN addgroup --system app && adduser --system --ingroup app app

COPY target/backend-0.0.1-SNAPSHOT.jar app.jar

RUN mkdir -p /app/uploads && chown -R app:app /app

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
