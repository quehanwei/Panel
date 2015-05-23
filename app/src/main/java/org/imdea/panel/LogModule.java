package org.imdea.panel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Sergio on 18/05/2015.
 */
public class LogModule {

    final String TAG = "LogModule";
    Context mcontext;
    boolean turnOff = false;
    String path = Environment.getExternalStorageDirectory().getPath() + "/";

    public LogModule(Context context) {
        Log.i(TAG, path);
        mcontext = context;
    }

    public void writeToLog(String filename, String title, String text) {
        try {

            FileWriter fstream = new FileWriter(path + filename, true);
            PrintWriter out = new PrintWriter(fstream);
            String s = new SimpleDateFormat("yyyy.MM.dd.HH:mm:ss").format(Calendar.getInstance().getTime()) + "\t" + title + "\t" + text + "\n";
            Log.i(TAG, s);
            out.print(s);
            out.close();
        } catch (Exception e) {

        }
    }

    public void getLocation() {
        Criteria criteria = new Criteria();
        LocationManager locationManager = (LocationManager) mcontext.getSystemService(mcontext.LOCATION_SERVICE);
        Location devicelocation = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, true));
        writeToLog("PanelLog", "GPS", devicelocation.getLatitude() + "\t" + devicelocation.getLongitude() + "\t" + devicelocation.getAccuracy() + "\t" + devicelocation.getTime());
    }

    public void writeTimings(long Starttime, long Connecttime, long Sendtime) {

        writeToLog("PanelLog", "TIME", String.valueOf(Connecttime - Starttime) + "\t" + String.valueOf(Sendtime - Connecttime));
        Log.i("TIME", String.valueOf(Connecttime - Starttime) + "\t" + String.valueOf(Sendtime - Connecttime));

    }

    public void getWifi() {
        turnOff = false;
        final WifiManager mWifiManager = (WifiManager) mcontext.getSystemService(mcontext.WIFI_SERVICE);
        if (mWifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
            turnOff = true;
            mWifiManager.setWifiEnabled(true);
        }

        if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {

            // register WiFi scan results receiver
            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

            mcontext.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    List<ScanResult> results = mWifiManager.getScanResults();
                    final int N = results.size();
                    String s = "";
                    for (ScanResult ap : results) {
                        writeToLog("PanelLog", "WIFI", ap.SSID + "\t" + ap.BSSID);
                    }

                    if (turnOff) mWifiManager.setWifiEnabled(false);

                    /*Log.v(TAG, "Wi-Fi Scan Results ... Count:" + N);
                    for(int i=0; i < N; ++i) {
                        Log.v(TAG, "  BSSID       =" + results.get(i).BSSID);
                        Log.v(TAG, "  SSID        =" + results.get(i).SSID);
                        Log.v(TAG, "  Capabilities=" + results.get(i).capabilities);
                        Log.v(TAG, "  Frequency   =" + results.get(i).frequency);
                        Log.v(TAG, "  Level       =" + results.get(i).level);
                        Log.v(TAG, "---------------");*/
                }
            }, filter);

            // start WiFi Scan
            mWifiManager.startScan();
        }

    }
}
