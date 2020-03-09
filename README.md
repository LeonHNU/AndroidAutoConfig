# AndroidAutoConfig
Android Device Automatic Configuration
  1. Go through SetupWizard
  2. Connect to wifi
  3. Minimize screen brightness
  4. Disable screen lock
  5. Enable stay awake
  6. Disable verify apps over USB
  7. enable backup
  8. chrome first open
  9. camera first open

How to generate apk from source code
  1. make sure you have installed android studio, android sdk, gradle.
  2. build apk by android studio or gradle
    2.1 use gradle for build
    2.2 use gradle for build. 
        cd AndroidAutoConfig
        gradlew build
        gradlew assembleAndroidTest
  3. copy apk to root dir
        ./copy_apk.sh

How to use this tool
  1. copy following file to your PC, all files in same dir
     AndroidAutoConfig.apk  AndroidAutoConfig.sh  AndroidAutoConfigTest.apk  copy_apk.sh  DeviceConfig.xml
  2. ./AndroidAutoConfig.sh -s SERIAL_NO -t TEST_MODULE
     Following modules are supported
       2.1 cts  -- android cts config
       2.2 gts  -- android gts config
       2.3 vts  -- android vts config
       2.4 perf -- android go perf test config
       2.5 fr   -- factory data reset
       2.6 frl  -- factory data reset loop