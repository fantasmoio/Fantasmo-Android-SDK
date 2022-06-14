#!/bin/sh

#  appcenter-post-build.sh
#  FantasmoSDK
#
#  Created by Fantasmo QA on 14/6/2022.
#

set -e # Stop running script if a command exits with a non-zero status

# Post Build Script run by AppCenter which launches Espresso Tests
if [ "$AGENT_JOBSTATUS" == "Succeeded" ]; then

    # Build the debug app and tests
    $APPCENTER_SOURCE_DIRECTORY/gradlew assembleDebug
    $APPCENTER_SOURCE_DIRECTORY/gradlew assembleAndroidTest
    # $APPCENTER_SOURCE_DIRECTORY/gradlew assembleRelease

    # submit the test command to AppCenter
    appcenter test run espresso \
      --app "fantasmo-qa/Android-Mobile-SDK" \
      --devices "fantasmo-qa/android-test-devices" \
      --app-path "$APPCENTER_SOURCE_DIRECTORY/app/build/outputs/apk/debug/app-debug.apk" \
      --test-series "master" \
      --locale "en_US" \
      --build-dir "$APPCENTER_SOURCE_DIRECTORY/app/build/outputs/apk/androidTest/debug" \
      --token $APPCENTER_TOKEN \
      --async
fi
