#!/bin/bash

# Define build directory
BUILD_DIR="build"

# Create build directory if it doesn't exist
mkdir -p "$BUILD_DIR"

# Clean previous builds
rm -rf "$BUILD_DIR"/*

# Compile and package the project
mvn clean package -DskipTests -o

# Move the generated JAR to the build directory
JAR_NAME="SMP Position Saver-1.0.0.jar"
if [ -f "target/$JAR_NAME" ]; then
    mv "target/$JAR_NAME" "$BUILD_DIR/"
    echo "Build successful. JAR moved to $BUILD_DIR/$JAR_NAME"
else
    echo "Build failed. JAR not found in target directory."
    exit 1
fi