#!/bin/bash
# PhpNativePlugin Builder - Standard DroidScript Plugin Format
# Hybrid PHP/Java/DroidScript Architecture Plugin
set -e

PLUGIN="PhpNativePlugin"
PKG_PATH="com/phpnative/plugins/user"
SRC="${PLUGIN}.java"

# Parse arguments
DEPLOY=false
CLEAN_ONLY=false
VERBOSE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -d|--deploy)    DEPLOY=true; shift ;;
        -c|--clean)     CLEAN_ONLY=true; shift ;;
        -v|--verbose)   VERBOSE=true; shift ;;
        -h|--help)
            echo "Usage: ./build.sh [options]"
            echo "  -d, --deploy   Deploy to connected Android device via ADB"
            echo "  -c, --clean    Clean build artifacts only"
            echo "  -v, --verbose  Show detailed output"
            echo "  -h, --help     Show this help"
            exit 0
            ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

echo "========================================"
echo "  ${PLUGIN} Builder"
echo "========================================"
echo ""

# Clean only mode
if [ "$CLEAN_ONLY" = true ]; then
    echo "-> Cleaning build artifacts..."
    rm -rf build out
    echo "   Done"
    exit 0
fi

# Find tools
JAVAC=$(which javac 2>/dev/null)
if [ -z "$JAVAC" ]; then echo "ERROR: javac not found"; exit 1; fi
echo "OK javac: $($JAVAC -version 2>&1)"

# Find Android SDK
SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/local/share/android-commandlinetools}}"
ANDROID_JAR=$(find "$SDK_ROOT/platforms" -name "android.jar" 2>/dev/null | sort -r | head -1)
if [ -z "$ANDROID_JAR" ]; then echo "ERROR: android.jar not found"; exit 1; fi
echo "OK android.jar: $ANDROID_JAR"

D8=$(find "$SDK_ROOT/build-tools" -name "d8" 2>/dev/null | sort -r | head -1)
if [ -z "$D8" ]; then echo "ERROR: d8 not found"; exit 1; fi
echo "OK d8: $D8"

echo ""

# Clean
rm -rf build out
mkdir -p build out

# Compile Java (with package → outputs to build/com/phpnative/plugins/user/)
echo "-> Compiling Java source..."
$JAVAC -source 11 -target 11 \
    -classpath "$ANDROID_JAR" \
    -d build \
    "$SRC"
echo "   OK Compiled"

# DEX
echo "-> Converting to DEX format..."
mkdir -p build/dex
$D8 --lib "$ANDROID_JAR" --output build/dex $(find build -name "*.class")
echo "   OK Created classes.dex"

# JAR (contains classes.dex)
echo "-> Packaging JAR..."
jar cf build/${PLUGIN}.jar -C build/dex classes.dex
echo "   OK Created ${PLUGIN}.jar"

# PPK (jar + inc + html + assets with PHP files and binaries)
echo "-> Building PPK plugin..."
cd out
cp ../build/${PLUGIN}.jar .
cp ../${PLUGIN}.inc .
cp ../${PLUGIN}.html .

# Include assets (PHP files only - exclude libphp.so binary)
if [ -d "../assets" ]; then
    mkdir -p assets
    # Copy PHP files only
    cp ../assets/*.php assets/ 2>/dev/null || true
    # Keep empty arch folders for structure (optional)
    mkdir -p assets/arm64-v8a assets/armeabi-v7a
    echo "   Included assets/ (PHP files only, libphp.so excluded)"
fi

# Build zip with all components
ZIP_CONTENTS="${PLUGIN}.jar ${PLUGIN}.inc ${PLUGIN}.html"
[ -d "assets" ] && ZIP_CONTENTS="$ZIP_CONTENTS assets/"
zip -r ${PLUGIN}.ppk $ZIP_CONTENTS
cd ..
echo "   OK Created ${PLUGIN}.ppk"

# Show PPK contents
echo ""
echo "-> PPK Contents:"
unzip -l out/${PLUGIN}.ppk | grep -E "^\s+[0-9]+" | head -20

echo ""
echo "========================================"
echo "  Build Complete!"
echo "========================================"
echo ""
echo "  Plugin:  $(pwd)/out/${PLUGIN}.ppk  ($(du -h out/${PLUGIN}.ppk | cut -f1))"
echo "  JAR:     $(pwd)/build/${PLUGIN}.jar  ($(du -h build/${PLUGIN}.jar | cut -f1))"
echo ""

# Deploy to device if requested
if [ "$DEPLOY" = true ]; then
    echo "-> Deploying to device..."
    ADB=$(which adb 2>/dev/null)
    if [ -z "$ADB" ]; then
        echo "   ERROR: adb not found. Install Android SDK platform-tools."
        exit 1
    fi
    
    # Check device connection
    DEVICE=$($ADB devices | grep -v "List" | grep "device$" | head -1 | cut -f1)
    if [ -z "$DEVICE" ]; then
        echo "   ERROR: No Android device connected"
        exit 1
    fi
    echo "   Device: $DEVICE"
    
    # Push to DroidScript plugins folder
    DEST="/sdcard/DroidScript/Plugins/${PLUGIN}.ppk"
    $ADB push "out/${PLUGIN}.ppk" "$DEST"
    echo "   OK Deployed to $DEST"
    echo ""
    echo "   Restart DroidScript to load the updated plugin"
    echo ""
else
    echo "  Install on device:"
    echo "    Copy ${PLUGIN}.ppk to /sdcard/DroidScript/Plugins/"
    echo "    or DroidScript Menu -> Plugins -> Install"
    echo "    or run: ./build.sh --deploy"
    echo ""
fi

echo "  Usage:"
echo "    app.LoadPlugin(\"${PLUGIN}\");"
echo "    var php = app.CreatePhpNative();"
echo "    php.SetOnReady(OnReady);"
echo "    php.Start();  // Calls PHP index() method"
echo ""
echo "  Architecture:"
echo "    PHP 8 (Brain) <-> Java (UI/Bridge) <-> DroidScript (Sensors)"
echo ""
