package org.imdea.panel.Wireless;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.List;


public class WifiService extends Service {

    public static List peers = new ArrayList();
    final String TAG = "WifiService";
    WifiModule wmodule;

    public WifiService() {
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        wmodule = new WifiModule(this);
        wmodule.start();

        /*final Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            public void run() {
                if(isConnected==false){
                }
            }

        }, 30000, 90000);*/

        return Service.START_NOT_STICKY;
    }

    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

}

