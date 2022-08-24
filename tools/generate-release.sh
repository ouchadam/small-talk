#! /bin/bash

./gradlew clean bundleRelease -Punsigned --no-daemon --no-configuration-cache --no-build-cache
RELEASE_AAB=app/build/outputs/bundle/release/app-release.aab

jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore .secrets/upload-key.jks \
  -storepass $1 \
  $RELEASE_AAB \
  key0