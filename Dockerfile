# Multi-stage build for minimal image size
FROM gradle:8-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle clean build --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built JAR
COPY --from=build /app/build/libs/*.jar app.jar

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Create logs directory
RUN mkdir -p logs && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
