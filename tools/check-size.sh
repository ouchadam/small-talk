#! /bin/bash

rm -f build/bundle-tmp/app.apks

./gradlew bundleRelease --no-configuration-cache -x uploadCrashlyticsMappingFileRelease

bundletool build-apks \
--device-spec=tools/device-spec.json \
--bundle=app/build/outputs/bundle/release/app-release.aab --output=build/bundle-tmp/app.apks

bundletool get-size total \
--apks=build/bundle-tmp/app.apks \
--human-readable-sizes