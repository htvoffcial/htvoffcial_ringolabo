#!/bin/bash
set -euo pipefail
export JAVA_HOME=$HOME/workspace/tools/jdk-17.0.21+9
export PATH=$JAVA_HOME/bin:$PATH
SDK=$HOME/workspace/tools/android-sdk
BT=$SDK/build-tools/34.0.0
ANDROID_JAR=$SDK/platforms/android-34/android.jar
PROJ=$HOME/workspace/apple-juice-lab
OUT=$PROJ/out
rm -rf "$OUT"; mkdir -p "$OUT/classes" "$OUT/dex" "$PROJ/output"

# 1) compile Java
javac -encoding UTF-8 -source 8 -target 8 -cp "$ANDROID_JAR" -d "$OUT/classes" \
  $(find "$PROJ/src" -name '*.java')
echo "javac OK"

# 2) dex
"$BT/d8" --min-api 26 --output "$OUT/dex" $(find "$OUT/classes" -name '*.class')
echo "d8 OK"

# 3) package resources + manifest
"$BT/aapt2" link -o "$OUT/unsigned.apk" -I "$ANDROID_JAR" \
  --manifest "$PROJ/AndroidManifest.xml" --min-sdk-version 26 --target-sdk-version 34
echo "aapt2 OK"

# 4) add dex
cd "$OUT/dex" && zip -q "$OUT/unsigned.apk" classes.dex && cd "$PROJ"

# 5) align + sign (debug key)
if [ ! -f "$PROJ/debug.keystore" ]; then
  keytool -genkeypair -v -keystore "$PROJ/debug.keystore" -storepass android -keypass android \
    -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1
fi
"$BT/zipalign" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
"$BT/apksigner" sign --ks "$PROJ/debug.keystore" --ks-pass pass:android --key-pass pass:android \
  --out "$PROJ/output/apple-juice-lab.apk" "$OUT/aligned.apk"
"$BT/apksigner" verify --verbose "$PROJ/output/apple-juice-lab.apk"
ls -lh "$PROJ/output/apple-juice-lab.apk"
echo "BUILD SUCCESS"
