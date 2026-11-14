#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")"

if ! javac -version >/dev/null 2>&1; then
    printf 'Install a Java Development Kit (JDK 17 or later), then run this again.\n'
    exit 1
fi

mkdir -p out
javac --release 17 -encoding UTF-8 -d out src/campuslibrary/*.java
exec java -Xdock:name="Campus Library" -cp out campuslibrary.CampusLibrary
