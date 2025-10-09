# Use OpenJDK 21 and install Maven
FROM openjdk:21-jdk-slim AS build

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy pom.xml first for better Docker layer caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -Dspring.profiles.active=prod

# Use OpenJDK for runtime
FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/customer-service-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
