FROM public.ecr.aws/amazoncorretto/amazoncorretto:21-alpine

WORKDIR /app

RUN apk add --no-cache curl tzdata

RUN addgroup -S app && adduser -S app -G app

COPY target/backend-0.0.1-SNAPSHOT.jar app.jar

RUN mkdir -p /app/uploads && chown -R app:app /app

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
