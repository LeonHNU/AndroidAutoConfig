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

print("Start setupwizard going through loop")

iLoop  = 1

while (True):
    print('---------- Begin %dth test for device: %s----------' % (iLoop, args.serialno))
    print(time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(time.time())))

    # Set some system settings
    os.system(ADBS + "shell svc power stayon true")
    os.system(ADBS + "shell locksettings set-disabled true")
    os.system(ADBS + "shell settings put system screen_brightness 0")
    os.system(ADBS + "shell settings put global verifier_verify_adb_installs 0")

    # Install uiautomator apk to prepare for go through setupwizard
    os.system(ADBS + "install -t AndroidAutoConfig.apk")
    os.system(ADBS + "install -t AndroidAutoConfigTest.apk")
    # Push config file to device. It includes configs for wifi ap and google account
    os.system(ADBS + "push DeviceConfig.xml /data/local/tmp/")

    # Wakeup
    os.system(ADBS + "shell input keyevent 224")
    time.sleep(5)

    # Go through setupwizard with all options skip
    os.system(ADBS + "shell am instrument -w -r -e debug false -e class leon.hnu.androidautoconfig.AndroidAutoConfigTest#testGoThroughSetupWizardPureByWhile  leon.hnu.androidautoconfig.test/androidx.test.runner.AndroidJUnitRunner")
    time.sleep(20)
    res = subprocess.run(["adb", "-s", args.serialno,  "shell", " dumpsys activity top | grep ACTIVITY"], capture_output=True, text=True)
    print("Top Activity Info After setupwizard done: " + res.stdout)
    if res.stdout.__contains__("com.android.launcher3"):
        print("---------- %dth test success ----------" % iLoop)
    else:
        print("---------- %dth test failed, unexpected top activity:%s ----------" % (iLoop, res.stdout))
        break
    
    # Factory data reset

    # Set device provisioned to enable factory data reset by UI when setupwizard in not gone through sucessfully.
    # In that case if we don't set this, settings will skip ResetDashboardFragment before SUW completed
    os.system(ADBS + "shell settings put global device_provisioned 1")
    # Do factory data reset by UI(compatible for both user userdebug and eng build)
    print('Start to do factory data reset')
    #os.system(ADBS + "shell am instrument -w -r -e debug false -e class leon.hnu.androidautoconfig.AndroidAutoConfigTest#tesFactoryReset leon.hnu.androidautoconfig.test/androidx.test.runner.AndroidJUnitRunner")
    os.system(ADBS + "root")
    os.system(ADBS + "shell am broadcast -a android.intent.action.FACTORY_RESET -f 0x10000000 -e android.intent.extra.REASON MasterClearConfirm android")
    print('Factory data reset Done')
    time.sleep(30)

    # Wait device to restart up
    os.system(ADBS + "wait-for-device")
    print('Device detected After Factory Data Reset')

    # Wait for setupwizard to be top activity,
    wait_10s_times = 0
    while (wait_10s_times <= 30):
        #res = subprocess.run(["adb", "-s", args.serialno,  "shell", " dumpsys activity top | grep ACTIVITY"], capture_output=True, text=True)
        #if res.stdout.__contains__("com.google.android.setupwizard"):
        res = subprocess.run(["adb", "-s", args.serialno,  "shell", " getprop sys.boot_completed"], capture_output=True, text=True)
        print("sys.boot_completeted: %s; length: %d" % (res.stdout, len(res.stdout)))
        if res.stdout.__eq__("1\n"):
            break
        time.sleep(10)
        wait_10s_times += 1
    if (wait_10s_times <= 12):
        print('Device started after Factory Data Reset')
    else:
        print('Device start time out, 5 minutes after we detected adb, but it still not show up')
        break

    iLoop += 1
