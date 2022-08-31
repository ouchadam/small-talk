#! /bin/bash

./gradlew clean bundleRelease -Punsigned --no-daemon --no-configuration-cache --no-build-cache

WORKING_DIR=app/build/outputs/bundle/release
RELEASE_AAB=$WORKING_DIR/app-release.aab

cp $RELEASE_AAB $WORKING_DIR/app-release-unsigned.aab

echo "signing $RELEASE_AAB"
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore .secrets/upload-key.jks \
  -storepass $1 \
  $RELEASE_AAB \
  key0
