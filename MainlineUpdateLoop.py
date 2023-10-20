#!/usr/bin/python
import argparse
import os
import time
import subprocess

parser = argparse.ArgumentParser(description="Mainline Update loop script parser")

parser.add_argument('--version', '-v', action='version', version='%(prog)s version: v1.0', help='show the version')
parser.add_argument('--serialno', '-s', type=str, help='Device\'s serial number')

args = parser.parse_args()

ADBS = 'adb '
if args.serialno is not None:
    ADBS = 'adb -s ' + args.serialno + ' '

print("Start Mainline Update Loop")

iLoop  = 1

while (True):
    print('---------- Begin %dth test ----------' % iLoop)
    print(time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(time.time())))

    # Set some system settings
    os.system(ADBS + "shell svc power stayon true")
    os.system(ADBS + "shell locksettings set-disabled true")
    os.system(ADBS + "shell settings put system screen_brightness 0")
    os.system(ADBS + "shell settings put global verifier_verify_adb_installs 0")

    # Install uiautomator apk to prepare for go through setupwizard and do mainline update
    os.system(ADBS + "install -t AndroidAutoConfig.apk")
    os.system(ADBS + "install -t AndroidAutoConfigTest.apk")
    # Push config file to device. It includes configs for wifi ap and google account
    os.system(ADBS + "push DeviceConfig.xml /data/local/tmp/")

    # Wakeup
    os.system(ADBS + "shell input keyevent 224")
    time.sleep(5)

    # Go through setupwizard with Google Acount login
    os.system(ADBS + "shell am instrument -w -r -e debug false -e class leon.hnu.androidautoconfig.AndroidAutoConfigTest#testGoThroughSetupWizardWithGoogleAccount leon.hnu.androidautoconfig.test/androidx.test.runner.AndroidJUnitRunner")
    time.sleep(50)
    res = subprocess.run(["adb", "-s", args.serialno,  "shell", " dumpsys activity top | grep ACTIVITY"], capture_output=True, text=True)
    print("Top Activity Info After setupwizard done: " + res.stdout)
    if res.stdout.__contains__("com.android.launcher3"):
        print("We have go through setupwizard and come to launcher")
    else:
        print("---------- %dth test failed for go through setupwizard Failed ----------" % iLoop)
        # Factory data reset
        os.system(ADBS + "root")
        os.system(ADBS + "shell am broadcast -a android.intent.action.FACTORY_RESET -f 0x10000000 -e android.intent.extra.REASON MasterClearConfirm android")
        print('Factory data reset Done, and wait 5 minutes')
        time.sleep(300)

        # Wait device to restart up
        os.system(ADBS + "wait-for-device")
        print('Device detected After Factory Date Reset')

        iLoop += 1
        continue

    # do mainline update
    os.system(ADBS + "shell am start -a android.intent.action.SAFETY_CENTER")
    time.sleep(20)
    # do Mainiline setup
    os.system(ADBS + "shell am instrument -w -r -e debug false -e class leon.hnu.androidautoconfig.AndroidAutoConfigTest#testDoMainlineUpdateScroll leon.hnu.androidautoconfig.test/androidx.test.runner.AndroidJUnitRunner")
    print('Mainiline Update Done')

    # Wait device to restart up
    time.sleep(30)
    os.system(ADBS + "wait-for-device")
    print('Device detected after Mainiline Update restart')
    time.sleep(60)

    # Factory data reset
    os.system(ADBS + "root")
    os.system(ADBS + "shell am broadcast -a android.intent.action.FACTORY_RESET -f 0x10000000 -e android.intent.extra.REASON MasterClearConfirm android")
    print('Factory data reset Done, and wait 5 minutes')
    time.sleep(300)

    # Wait device to restart up
    os.system(ADBS + "wait-for-device")
    print('Device detected After Factory Date Reset')
    print('---------- End %dth test: success ----------')

    iLoop += 1
