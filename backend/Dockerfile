# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY src/ ./src/
RUN javac -d out src/*.java

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/out ./out
RUN mkdir -p /app/data
ENV PORT=8080
EXPOSE 8080
VOLUME ["/app/data"]
CMD ["java", "-cp", "out", "ApiServer"]
