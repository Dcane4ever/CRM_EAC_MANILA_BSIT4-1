#!/bin/bash

# Start script for Render deployment
echo "🚀 Starting CRM Customer Service Application..."

# Run the Spring Boot application with production profile
java -Dspring.profiles.active=prod -jar target/customer-service-*.jar
