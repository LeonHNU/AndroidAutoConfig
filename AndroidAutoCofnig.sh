#!/bin/bash

ADBS=adb
MODULE=

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

# get parameters
# -s: serialno
# -m: module name
while getopts "s:m:" arg
do
    case $arg in
      s)
        ADBS="adb -s $OPTARG"
        ;;
      m)
        MODULE=$OPTARG
        ;;
      ?)
        echo "Unknow argument: $arg"
        exit 1
        ;;
    esac
done

# Check if we can connect to the right device. If not, force exit.
checkDeviceConnection

# set screen brightness to lowest
minBrightness=`$ADBS shell dumpsys power | grep mScreenBrightnessSettingMinimum | sed 's/.*=//g'`
$ADBS shell settings put system screen_brightness $minBrightness

# set screen lock to none
$ADBS shell locksettings set-disabled true

# dismiss keyguard if already locked
$ADBS shell wm dismiss-keyguard

# set stay awake
$ADBS shell settings put global stay_oon_while_plugged_in 7
# or $ADBS shell svc power stayon usb 

# disable verify apps over usb
$ADBS shell settings put global verifier_verify_adb_installs 0

# light up screen if  
screenState=`$ADBS shell dumpsys window policy | grep screenState | sed 's/.*screenState=//g'`
if [ "$screenState" == "SCREEN_STATE_OFF" ]; then
	$ADBS shell input keyevent 26
fi

# enable wifi
$ADBS shell svc wifi enable






