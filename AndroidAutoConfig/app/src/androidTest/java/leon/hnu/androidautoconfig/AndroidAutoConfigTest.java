package leon.hnu.androidautoconfig;

import android.app.backup.BackupManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Xml;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class AndroidAutoConfigTest {
    private final String TAG = "AndroidAutoConfig";
    private final String PACKAGE_NAME_LAUNCHER = "com.android.launcher3";
    private final String PACKAGE_NAME_SETTINGS = "com.android.settings";
    private final String PACKAGE_NAME_SETUPWIZARD = "com.google.android.setupwizard";
    private final int WINDOW_UPDATE_TIMEOUT = 5 * 1000;
    private Context mContext;
    private UiDevice mUiDevice;
    private ConnectivityManager mConnectivityManager;
    private String wifiApName;
    private String wifiApPwd;

    @Before
    public void setUp() throws IOException, XmlPullParserException {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mUiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        getDeviceConfig();
    }

    private void startActivity(String packageName, String activityName, String subActivityName) {
        final String PARAM_NAME_SUBSETTINGS_EXTRA = ":android:show_fragment";
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.setComponent(new ComponentName(packageName, activityName));
        if (subActivityName != null) {
            intent.putExtra(PARAM_NAME_SUBSETTINGS_EXTRA, subActivityName);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    @Test
    public void testConnectWifi() {
        final int MAX_RETRY = 3;
        int retryTimes = 0;

        while (!isWifiConnected() && (retryTimes < MAX_RETRY)) {
            Log.d(TAG, "Try connect WIFI for " + (retryTimes + 1) + " time");
            try {
                connectWifi(wifiApName, wifiApPwd);
                if (!isWifiConnected()) {
                    Thread.sleep(10000);
                }
            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }
            retryTimes++;
        }

        assertTrue(retryTimes < MAX_RETRY);
    }

    private boolean isWifiConnected() {
        NetworkInfo wifiNetworkInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifiNetworkInfo != null && wifiNetworkInfo.isConnected();
    }

    private void connectWifi(String apName, String apPassword) throws InterruptedException, UiObjectNotFoundException {

        mContext.startActivity(new Intent().setAction(WifiManager.ACTION_PICK_WIFI_NETWORK).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        Thread.sleep(3000);
        mUiDevice.waitForWindowUpdate(PACKAGE_NAME_SETTINGS, WINDOW_UPDATE_TIMEOUT);
        UiObject2 wifiSwitch = mUiDevice.findObject(By.res(PACKAGE_NAME_SETTINGS,"switch_widget"));

        final int MAX_RETRY = 5;
        int retryTimes =0;
        while (!wifiSwitch.isChecked() && (retryTimes < MAX_RETRY)) {
            wifiSwitch.click();
            Thread.sleep(5000);
            retryTimes++;
        }

        if (retryTimes >= MAX_RETRY) {
            Log.d(TAG,"connectWifi: enable WIFI failed for 5 times, return");
            return;
        }

        UiScrollable apList = new UiScrollable(new UiSelector().className("androidx.recyclerview.widget.RecyclerView"));
        UiObject apObject = apList.getChildByText(new UiSelector().className("android.widget.TextView"),apName);
        apObject.click();
        mUiDevice.waitForWindowUpdate(PACKAGE_NAME_SETTINGS, WINDOW_UPDATE_TIMEOUT);

        UiObject2 pwdObj = mUiDevice.findObject(By.res(PACKAGE_NAME_SETTINGS, "password"));
        pwdObj.setText(apPassword);
        mUiDevice.waitForWindowUpdate(PACKAGE_NAME_SETTINGS, WINDOW_UPDATE_TIMEOUT);

        mUiDevice.findObject(By.text("CONNECT")).click();
        mUiDevice.waitForWindowUpdate(PACKAGE_NAME_SETTINGS, WINDOW_UPDATE_TIMEOUT);
    }

    @Test
    public void testEnableBackup() throws Exception {
        final String ACTION_BACKUP = "com.google.android.gms.backup.ACTION_BACKUP_SETTINGS";
        final String PACKAGE_NAME_GMS = "com.google.android.gms";

        mContext.startActivity(new Intent().setAction(ACTION_BACKUP).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        mUiDevice.waitForWindowUpdate(PACKAGE_NAME_GMS, WINDOW_UPDATE_TIMEOUT);

        waitforTextAndClick("Backup");
        mUiDevice.waitForWindowUpdate(PACKAGE_NAME_GMS, WINDOW_UPDATE_TIMEOUT);

        waitforTextAndClick("Back up to Google Drive");
        mUiDevice.waitForWindowUpdate(PACKAGE_NAME_GMS, WINDOW_UPDATE_TIMEOUT);
    }

    @Test
    public void testChromeFirstOpen() throws Exception {
        final String PACKAGE_NAME_CHROME = "com.android.chrome";
        final String ACTIVITY_NAME_CHROME = "com.google.android.apps.chrome.Main";

        mContext.startActivity(new Intent().setAction(Intent.ACTION_MAIN).setComponent(new ComponentName(PACKAGE_NAME_CHROME, ACTIVITY_NAME_CHROME)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        Thread.sleep(5000);
        mUiDevice.waitForWindowUpdate(PACKAGE_NAME_CHROME, WINDOW_UPDATE_TIMEOUT);

        waitforTextAndClick("Accept & continue");
        mUiDevice.waitForWindowUpdate(PACKAGE_NAME_CHROME, WINDOW_UPDATE_TIMEOUT);

        try {
            waitforTextAndClick("Next");
            mUiDevice.waitForWindowUpdate(PACKAGE_NAME_CHROME, WINDOW_UPDATE_TIMEOUT);
        } catch (Exception e) {
            Log.d(TAG, "Can not find \"Next\" button during testChromeFirstOpen, skip");
        }

        waitforTextAndClick("No thanks");
        mUiDevice.waitForWindowUpdate(PACKAGE_NAME_CHROME, WINDOW_UPDATE_TIMEOUT);
    }

    @Test
    public void testCameraFirstOpen() throws Exception {
        final String PACKAGE_NAME_CAMERA = "com.android.camera2";

        mContext.startActivity(new Intent().setAction(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        Thread.sleep(2000);
        mUiDevice.waitForWindowUpdate(PACKAGE_NAME_CAMERA, WINDOW_UPDATE_TIMEOUT);

        waitforTextAndClick("DENY");
        mUiDevice.waitForWindowUpdate(PACKAGE_NAME_CAMERA, WINDOW_UPDATE_TIMEOUT);

        waitforTextAndClick("NEXT");
        mUiDevice.waitForWindowUpdate(PACKAGE_NAME_CAMERA, WINDOW_UPDATE_TIMEOUT);
    }

    @Test
    public void testGoThroughSetupWizard() throws UiObjectNotFoundException {
        final String LOCALE_US_LANGUAGE = "English";
        final String LOCALE_US_COUNTRY = "United States";
        final String LOCALE_US_DISPLAY_NAME = LOCALE_US_LANGUAGE + " (" + LOCALE_US_COUNTRY + ")";
        UiObject2 obj;

        String currentPackageName = mUiDevice.getCurrentPackageName();
        // If system language is English(United States), then change to it.
        if(!mContext.getResources().getConfiguration().locale.getDisplayName().equals(LOCALE_US_DISPLAY_NAME)) {
            if (currentPackageName.equals(PACKAGE_NAME_SETUPWIZARD)) {
                setSetupWizardLocale(LOCALE_US_LANGUAGE, LOCALE_US_COUNTRY);
            } else {
                // to be done. If SetupWizard has already gone through, find other ways to change system language
            }
        }

        boolean inSetupWizard = false;
        while (currentPackageName.equals(PACKAGE_NAME_SETUPWIZARD) || (inSetupWizard && !currentPackageName.equals(PACKAGE_NAME_LAUNCHER))) {
            inSetupWizard = true;
            mUiDevice.waitForWindowUpdate(PACKAGE_NAME_SETUPWIZARD, WINDOW_UPDATE_TIMEOUT);
            currentPackageName = mUiDevice.getCurrentPackageName();

            obj = mUiDevice.findObject(By.clazz("android.widget.Button").text("START"));
            if (obj != null) {
                obj.click();
                continue;
            }

            obj = mUiDevice.findObject(By.clazz("android.widget.Button").text("Skip"));
            if (obj != null) {
                obj.click();
                continue;
            }

            obj = mUiDevice.findObject(By.clazz("android.widget.Button").text("CONTINUE"));
            if (obj != null) {
                obj.click();
                continue;
            }

            obj = mUiDevice.findObject(By.clazz("android.widget.Button").text("Next"));
            if (obj != null) {
                obj.click();
                continue;
            }

            obj = mUiDevice.findObject(By.clazz("android.widget.Button").text("More"));
            if (obj != null) {
                obj.click();
                continue;
            }

            obj = mUiDevice.findObject(By.clazz("android.widget.Button").text("Accept"));
            if (obj != null) {
                obj.click();
                continue;
            }

            obj = mUiDevice.findObject(By.clazz("android.widget.Button").text("Skip anyway"));
            if (obj != null) {
                obj.click();
                continue;
            }
        }
    }

    private void setSetupWizardLocale(String language, String country) throws UiObjectNotFoundException {
        mUiDevice.findObject(By.res(PACKAGE_NAME_SETUPWIZARD, "language_picker")).click();
        mUiDevice.waitForWindowUpdate(PACKAGE_NAME_SETUPWIZARD, WINDOW_UPDATE_TIMEOUT);

        UiScrollable languageList = new UiScrollable(new UiSelector().className("android.widget.ListView"));
        languageList.getChildByText(new UiSelector().className("android.widget.TextView"), language).click();
        mUiDevice.waitForWindowUpdate(PACKAGE_NAME_SETUPWIZARD, WINDOW_UPDATE_TIMEOUT);

        UiScrollable countryList = new UiScrollable(new UiSelector().className("android.widget.ListView"));
        countryList.getChildByText(new UiSelector().className("android.widget.TextView"), country).click();
        mUiDevice.waitForWindowUpdate(PACKAGE_NAME_SETUPWIZARD, WINDOW_UPDATE_TIMEOUT);
    }

    @Test
    public void testConfigCTS() throws Exception {
        testGoThroughSetupWizard();
        testConnectWifi();
        testChromeFirstOpen();
        testCameraFirstOpen();
        testCloseAllApps();
    }

    @Test
    public void testConfigGTS() throws Exception {
        testGoThroughSetupWizard();
        testConnectWifi();
        testEnableBackup();
        testChromeFirstOpen();
        testCameraFirstOpen();
        testCloseAllApps();
    }

    @Test
    public void testConfigVTS() throws Exception {
        testConnectWifi();
        testCameraFirstOpen();
        testCloseAllApps();
    }

    /* Use JAVA reflect to set system locale.
     * permission： android.permission.CHANGE_CONFIGURATION needed
     * but this permission is only granted to system app
     */
    public void changeSystemLocale(String language, String country) {
        try {
            Class iActivityManager = Class.forName("android.app.IActivityManager");
            Class activityManagerNative = Class.forName("android.app.ActivityManagerNative");
            Method getDefault = activityManagerNative.getDeclaredMethod("getDefault");
            Object objIActMag = getDefault.invoke(activityManagerNative);
            Method getConfiguration = iActivityManager.getDeclaredMethod("getConfiguration");
            Configuration config = (Configuration) getConfiguration.invoke(objIActMag);
            if (config != null) {
                config.locale = new Locale(language, country);
            } else {
                Log.d(TAG, "changeSystemLocale: failed to get configuration");
                return;
            }
            Class clzConfig = Class.forName("android.content.res.Configuration");
            java.lang.reflect.Field userSetLocale = clzConfig.getField("userSetLocale");
            userSetLocale.set(config, true);
            Class[] clzParams = {Configuration.class};
            Method updateConfiguration = iActivityManager.getDeclaredMethod("updateConfiguration", clzParams);
            updateConfiguration.invoke(objIActMag, config);
            BackupManager.dataChanged("com.android.providers.settings");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testLogCurActivity() {
        Log.d(TAG, mUiDevice.getCurrentPackageName() + "; " + mUiDevice.getCurrentActivityName());
    }

    @Test
    public void testCloseAllApps() throws Exception {
        UiObject2 mObject =  mUiDevice.findObject(By.res(PACKAGE_NAME_LAUNCHER,"overview_panel"));
        if (mObject == null) {
            mUiDevice.pressRecentApps();
            Thread.sleep(2000);
            mObject =  mUiDevice.findObject(By.res(PACKAGE_NAME_LAUNCHER,"overview_panel"));
        }

        if (mObject != null) {
            mObject = mUiDevice.findObject(By.res(PACKAGE_NAME_LAUNCHER, "clear_all_button"));
            if (mObject != null) {
                mObject.click();
                mUiDevice.waitForIdle();
            } else {
                final int MAX_RECENT_APP = 20;
                final int DISPLAY_HIGHT = mUiDevice.getDisplayHeight();
                final int DISPLAY_WIDTH = mUiDevice.getDisplayWidth();

                int tryNum = 0;
                mObject = mUiDevice.findObject(By.res(PACKAGE_NAME_LAUNCHER, "clear_all"));
                while ((mObject == null) && (tryNum < MAX_RECENT_APP)) {
                    mUiDevice.swipe(DISPLAY_WIDTH / 4, DISPLAY_HIGHT/2, DISPLAY_HIGHT * 3 / 4, DISPLAY_WIDTH / 2, 5);
                    mUiDevice.waitForIdle();
                    mObject = mUiDevice.findObject(By.res(PACKAGE_NAME_LAUNCHER, "clear_all"));
                    tryNum ++;
                }

                if (mObject != null) {
                    mObject.click();
                } else {
                    throw new Exception("Failed to find clear all button after max retry times for normal recents device");
                }
            }
        } else {
            throw new Exception("Failed to find recents overview");
        }
    }

    @Test
    public void tesFactoryReset() throws Exception {
        final String ACTIVITY_NAME_SETTINGS = "com.android.settings.Settings";

        mContext.startActivity(new Intent().setComponent(new ComponentName(PACKAGE_NAME_SETTINGS, ACTIVITY_NAME_SETTINGS)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        Thread.sleep(2000);
        mUiDevice.waitForWindowUpdate(PACKAGE_NAME_SETTINGS, WINDOW_UPDATE_TIMEOUT);

        UiScrollable settingsList = new UiScrollable(new UiSelector().className("androidx.recyclerview.widget.RecyclerView"));
        UiObject systemObject = settingsList.getChildByText(new UiSelector().className("android.widget.TextView"), "System");
        systemObject.click();
        mUiDevice.waitForWindowUpdate(PACKAGE_NAME_SETTINGS, WINDOW_UPDATE_TIMEOUT);

        try {
            waitforTextAndClick("Advanced");
            mUiDevice.waitForWindowUpdate(PACKAGE_NAME_SETTINGS, WINDOW_UPDATE_TIMEOUT);
        } catch (Exception e) {
            Log.d(TAG, "Can not find \"Advanced\" button during tesFactoryReset, skip");
        }

        waitforTextAndClick("Reset options");
        mUiDevice.waitForWindowUpdate(PACKAGE_NAME_SETTINGS, WINDOW_UPDATE_TIMEOUT);

        waitforTextAndClick("Erase all data (factory reset)");
        mUiDevice.waitForWindowUpdate(PACKAGE_NAME_SETTINGS, WINDOW_UPDATE_TIMEOUT);

        waitforTextAndClick("Erase all data");
        mUiDevice.waitForWindowUpdate(PACKAGE_NAME_SETTINGS, WINDOW_UPDATE_TIMEOUT);

        waitforTextAndClick("Erase all data");
    }

    private void waitforTextAndClick(String text) throws Exception {
        final int DEF_WAIT_SECONDS = 10;
         waitforTextAndClick(text, DEF_WAIT_SECONDS);
    }

    private void waitforTextAndClick(String text, int waitTimeSecond) throws Exception {
        int retryTimes = 0;
        UiObject2 textObj = mUiDevice.findObject(By.text(text));

        while((textObj == null) && (retryTimes <= waitTimeSecond)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            textObj = mUiDevice.findObject(By.text(text));
            retryTimes++;
        }

        if (textObj != null) {
            textObj.click();
        } else {
            throw new Exception("Wait for text: " + text + ", error");
        }
    }

    private void getDeviceConfig() throws XmlPullParserException, IOException {
        final String CFG_FILE = "/data/local/tmp/DeviceConfig.xml";
        final String TAG_WIFI_AP_NAME  = "wifi_ap_name";
        final String TAG_WIFI_AP_PWD = "wifi_ap_pwd";

        File cfgFile = new File(CFG_FILE);
        FileInputStream cfgFileIS = new FileInputStream(cfgFile);

        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(cfgFileIS, "UTF-8");

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals(TAG_WIFI_AP_NAME)) {
                    parser.next();
                    wifiApName = parser.getText();
                } else if (parser.getName().equals(TAG_WIFI_AP_PWD)) {
                    parser.next();
                    wifiApPwd = parser.getText();
                }
            }
            eventType = parser.next();
        }
    }
}
