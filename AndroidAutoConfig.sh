#!/bin/bash

ADBS=adb
TEST=
LOCAL_DIR=`dirname $0`
DEVICE_TMP_DIR=/data/local/tmp/
CONFIG_FILE_NAME=DeviceConfig.xml

# Check if we can connect to the right device. There are three cases
# in which we can not connect to the right device and force exit:
#   1. no device is connected;
#   2. device with given serialno is not connected
#   3. serialno is not given and there are more than one connected device.
checkDeviceConnection() {
    outputLine=`$ADBS shell uname 2>&1 | grep error`
    if [ "$outputLine" != "" ]; then
        echo Device connection check failed: $outputLine
        exit 1
    fi
}

performShellConfig() {
    # set screen brightness to lowest
    minBrightness=`$ADBS shell dumpsys power | grep mScreenBrightnessSettingMinimum | sed 's/.*=//g'`
    $ADBS shell settings put system screen_brightness $minBrightness

    # set screen lock to none
    $ADBS shell locksettings set-disabled true

    # dismiss keyguard if already locked
    $ADBS shell wm dismiss-keyguard

    # set stay awake
    $ADBS shell settings put global stay_on_while_plugged_in 7
    # or $ADBS shell svc power stayon usb

    # disable verify apps over usb
    $ADBS shell settings put global verifier_verify_adb_installs 0

    # light up screen if screen is off
    screenState=`$ADBS shell dumpsys window policy | grep screenState | sed 's/.*screenState=//g'`
    if [ "$screenState" == "SCREEN_STATE_OFF" ]; then
        $ADBS shell input keyevent 26
    fi

    # enable wifi
    $ADBS shell svc wifi enable
}

waitForProp() {
    prop_name=$1
    prop_value_expected=$2
    prop_value_real=`$ADBS shell getprop $prop_name`

    echo `date "+%Y-%m-%d %H:%M:%S"` "Wait for prop: $prop_name to have value: $prop_value_expected"

    while [ "$prop_value_real" != "$prop_value_expected" ]
    do
        sleep 1
        prop_value_real=`$ADBS shell getprop $prop_name`
    done

    echo `date "+%Y-%m-%d %H:%M:%S"` "$prop_name have got value: $prop_value_expected"
}

factoryRestLoop() {
    echo "start factory reset loop"

    resetTimes=0
    while true
    do
        resetTimes=$[ resetTimes + 1]
        echo "factory reset loop: $resetTimes"
        performShellConfig
        $ADBS push $LOCAL_DIR/$CONFIG_FILE_NAME $DEVICE_TMP_DIR
        $ADBS install -t  $LOCAL_DIR/AndroidAutoConfig.apk
        $ADBS install -t  $LOCAL_DIR/AndroidAutoConfigTest.apk
        $ADBS shell am instrument -w -r -e debug false -e class 'leon.hnu.androidautoconfig.AndroidAutoConfigTest#tesFactoryReset' leon.hnu.androidautoconfig.test/androidx.test.runner.AndroidJUnitRunner
        sleep 30
        $ADBS wait-for-device
        waitForProp sys.boot_completed 1
        sleep 10
    done
}

performTestConfig() {
    testModule=$1
    configFunc=
    if [ "$testModule" == "cts" ]; then
        configFunc=testConfigCTS
    elif [ "$testModule" == "gts" ]; then
        configFunc=testConfigGTS
    elif [ "$testModule" == "vts" ]; then
        configFunc=testConfigVTS
    elif [ "$testModule" == "perf" ]; then
        configFunc="testGoThroughSetupWizard"
    elif [ "$testModule" == "fr" ]; then
        configFunc="tesFactoryReset"
    fi

    if [ "$configFunc" != "" ]; then
        performShellConfig
        $ADBS push $LOCAL_DIR/$CONFIG_FILE_NAME $DEVICE_TMP_DIR
        $ADBS uninstall leon.hnu.androidautoconfig
        $ADBS uninstall leon.hnu.androidautoconfig.test
        $ADBS install -t  $LOCAL_DIR/AndroidAutoConfig.apk
        $ADBS install -t  $LOCAL_DIR/AndroidAutoConfigTest.apk
        $ADBS shell am instrument -w -r -e debug false -e class leon.hnu.androidautoconfig.AndroidAutoConfigTest#$configFunc leon.hnu.androidautoconfig.test/androidx.test.runner.AndroidJUnitRunner
        $ADBS uninstall leon.hnu.androidautoconfig
        $ADBS uninstall leon.hnu.androidautoconfig.test
        $ADBS shell rm $DEVICE_TMP_DIR/$CONFIG_FILE_NAME
    fi
}

# get parameters
# -s: serialno
# -m: module name
while getopts "s:t:" arg
do
    case $arg in
      s)
        ADBS="adb -s $OPTARG"
        ;;
      t)
        TEST=$OPTARG
        ;;
      ?)
        echo "Unknow argument: $arg"
        exit 1
        ;;
    esac
done

# Check if we can connect to the right device. If not, force exit.
checkDeviceConnection

if [ "$TEST" == "frl" ]; then
    factoryRestLoop
else
    performTestConfig $TEST
fi