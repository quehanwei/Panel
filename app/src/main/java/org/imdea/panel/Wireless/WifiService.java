package org.imdea.panel.Wireless;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import org.imdea.panel.Bluetooth.BtModule;
import org.imdea.panel.Global;


public class WifiService extends Service {

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Global.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BtModule.STATE_CONNECTED:
                            break;
                        case BtModule.STATE_CONNECTING:
                            break;
                        case BtModule.STATE_LISTEN:
                            break;
                        case BtModule.STATE_NONE:
                            break;
                    }
                    break;
                case Global.MESSAGE_WRITE:
                    //byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    //String writeMessage = new String(writeBuf);
                    break;
                case Global.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);                      // construct a string from the valid bytes in the buffer
                    //parseJson(readMessage); // We decrypt the message and insert it into the database
                    sendBroadcast(new Intent("org.imdea.panel.MSG_RECEIVED")); // Now we warn the app that we received a new message
                    break;
                case Global.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    break;
                case Global.MESSAGE_TOAST:

                    break;
                case Global.CONECTION_FAILED:
                    break;
            }
        }
    };
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    WiFiDirectBroadcastReceiver Receiver;


    public WifiService() {
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        Receiver = new WiFiDirectBroadcastReceiver(mManager, mChannel);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        return Service.START_NOT_STICKY;
    }

    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

}

