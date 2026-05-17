#!/bin/bash
# Start the NMIT-POS client
cd "$(dirname "$0")"

# Use JetBrains Runtime — same JVM IDEA uses for CalcTerm/CalcPlus.
# JBR has native Wayland HiDPI support (xwayland-native-scaling) built in;
# system OpenJDK does not, which is why everything looked tiny.
JBR_HOME="$HOME/.local/share/JetBrains/Toolbox/apps/intellij-idea/jbr"
if [ -x "$JBR_HOME/bin/java" ]; then
    JAVA="$JBR_HOME/bin/java"
    JAVAC="$JBR_HOME/bin/javac"
else
    echo "JBR not found at $JBR_HOME — falling back to system java (may look small on HiDPI Wayland)"
    JAVA="java"
    JAVAC="javac"
fi

LIBS="lib/*"
SRC="pos-client/src"
OUT="pos-client/out"

mkdir -p "$OUT"

echo "Compiling client..."
find "$SRC" -name "*.java" > /tmp/client_sources.txt
"$JAVAC" -cp "$LIBS" -d "$OUT" @/tmp/client_sources.txt

if [ $? -eq 0 ]; then
    # Copy resource files to out (properties + images)
    find "$SRC" \( -name "*.properties" -o -name "*.png" -o -name "*.jpg" -o -name "*.svg" -o -name "*.gif" \) | while read f; do
        rel="${f#$SRC/}"
        mkdir -p "$OUT/$(dirname "$rel")"
        cp "$f" "$OUT/$rel"
    done

    echo "Starting client with JBR: $JAVA"
    "$JAVA" \
        -Dawt.useSystemAAFontSettings=lcd \
        -Dswing.aatext=true \
        -cp "$OUT:$LIBS" App
else
    echo "Compilation failed."
fi
