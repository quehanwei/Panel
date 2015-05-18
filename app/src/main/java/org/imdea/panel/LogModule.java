package org.imdea.panel;

import android.net.wifi.WifiManager;
import android.os.Environment;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Sergio on 18/05/2015.
 */
public class LogModule {

    WifiManager wifi;

    public void writeToLog(String s) {
        try {
            FileWriter fstream = new FileWriter(Environment.getExternalStorageDirectory().getPath() + "/panel_log", true);
            PrintWriter out = new PrintWriter(fstream);
            out.print(new SimpleDateFormat("yyyy.MM.dd.HH:mm:ss").format(Calendar.getInstance().getTime()) + "\t" + s);
            out.close();
        } catch (Exception e) {

        }
    }

    public void getLocation() {
    }

    public void getWifi() {

    }
}
