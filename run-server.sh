#!/bin/bash
# Start the NMIT-POS server
cd "$(dirname "$0")"

LIBS="lib/*"
SRC="pos-server/src"
OUT="pos-server/out"

mkdir -p "$OUT"

echo "Compiling server..."
find "$SRC" -name "*.java" > /tmp/server_sources.txt
javac -cp "$LIBS" -d "$OUT" @/tmp/server_sources.txt

if [ $? -eq 0 ]; then
    echo "Starting server on port 9090..."
    java -cp "$OUT:$LIBS" server.POSServer
else
    echo "Compilation failed."
fi
