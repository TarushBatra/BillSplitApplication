    # Use Eclipse Temurin 17 as base image
    FROM eclipse-temurin:17-jdk-jammy

# Set working directory
WORKDIR /app

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy pom.xml first for better caching
COPY pom.xml .

# Download dependencies (skip strict offline to avoid resolution issues)
RUN mvn -q -e -DskipTests dependency:resolve dependency:resolve-plugins || true

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "target/bill-split-backend-0.0.1-SNAPSHOT.jar"]

