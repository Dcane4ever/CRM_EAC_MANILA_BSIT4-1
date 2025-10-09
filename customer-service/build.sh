#!/bin/bash

# Build script for Render deployment
echo "🔨 Building CRM Customer Service Application..."

# Build with Maven
./mvnw clean package -DskipTests -Dspring.profiles.active=prod

echo "✅ Build completed successfully!"
