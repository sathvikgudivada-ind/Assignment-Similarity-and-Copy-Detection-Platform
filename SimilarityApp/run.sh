#!/bin/sh
# Compiles and starts the website version (Linux / macOS).
cd "$(dirname "$0")"
mkdir -p out
javac -encoding UTF-8 -d out src/*.java && java -cp "out:lib/*" SimilarityWeb
