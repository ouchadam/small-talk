#! /bin/bash
set -e

WORKING_DIR=app/build/outputs/apk/release
UNSIGNED=$WORKING_DIR/app-foss-release-unsigned.apk
ALIGNED_UNSIGNED=$WORKING_DIR/app-foss-release-unsigned-aligned.apk
SIGNED=$WORKING_DIR/app-foss-release-signed.apk

ZIPALIGN=$(find "$ANDROID_HOME" -iname zipalign -print -quit)
APKSIGNER=$(find "$ANDROID_HOME" -iname apksigner -print -quit)

./gradlew assembleRelease -Pfoss -Punsigned --no-daemon --no-configuration-cache --no-build-cache

$ZIPALIGN -v -p 4 $UNSIGNED $ALIGNED_UNSIGNED

$APKSIGNER sign \
  --ks .secrets/fdroid.keystore \
  --ks-key-alias key0 \
  --ks-pass pass:$1 \
  --out $SIGNED \
  $ALIGNED_UNSIGNED
