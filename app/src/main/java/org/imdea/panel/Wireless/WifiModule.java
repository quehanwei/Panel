package org.imdea.panel.Wireless;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class WifiModule extends BroadcastReceiver implements WifiP2pManager.ChannelListener, WifiP2pManager.ActionListener, WifiP2pManager.ConnectionInfoListener, WifiP2pManager.GroupInfoListener,
        WifiP2pManager.PeerListListener {


    public static List peers = new ArrayList();
    final int SERVER_PORT = 4576;
    final String TAG = "WifiModule";
    WifiP2pManager mManager;
    IntentFilter mIntentFilter;
    SharedPreferences SP;
    WifiP2pManager.Channel mChannel;
    String my_MAC;
    Context context;

    WifiModule(Context context) {
        this.context = context;
    }

    public void start() {

        Log.i(TAG, "Starting Wifi Module");
        setIsWifiP2pEnabled(true);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        WifiManager wMan = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        my_MAC = wMan.getConnectionInfo().getMacAddress();
        Log.i(TAG, "MAC " + my_MAC);

        mManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(context, context.getMainLooper(), this);

        LocalBroadcastManager.getInstance(context).registerReceiver(this, new IntentFilter("START_WIFI_SCAN"));
        context.registerReceiver(this, mIntentFilter);

        SP = PreferenceManager.getDefaultSharedPreferences(context);

        mManager.discoverPeers(mChannel, this);
    }

    @Override
    public void onChannelDisconnected() {
        Log.w(TAG, "CMGR: Channel disconnected ");

    }

    @Override
    public void onFailure(int reason) {
        String text = "Failure: ";
        switch (reason) {
            case WifiP2pManager.P2P_UNSUPPORTED:
                text = text + " unsupported";
                break;
            case WifiP2pManager.BUSY:
                text = text + " busy";
                break;
            case WifiP2pManager.ERROR:
                text = text + " error";
                break;
        }

        Log.e(TAG, text);
    }

    @Override
    public void onSuccess() {
        Log.i(TAG, "Success");
    }

    @Override
    //this is the method that gets called when a broadcast intent is thrown
    //by the android framework that we registered for during initialization
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        switch (action) {
            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:          //thrown after a success full discoverPeers call
                //get the peer list via the onPeersChanged callback
                Log.i(TAG, "Received peers-changed-action");
                mManager.requestPeers(mChannel, this);
                break;
            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:             //thrown when a connection is established/broken

                Log.i(TAG, "Received connection-changed-action");

                //grab the network info object and check if a connection was established
                NetworkInfo networkinfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                Log.i(TAG, networkinfo.toString());
                if (networkinfo.isConnected()) {
                    Log.w(TAG, "Device Connected!");
                    mManager.requestConnectionInfo(mChannel, this);
                }
                break;

            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:              //this is thrown when the wifi direct state changes on the device

                //Log.i(TAG,"Received state-changed-action");

                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_DISABLED) {
                    Log.i(TAG, "WIFI_p2p_DISABLED");

                    setIsWifiP2pEnabled(true);
                } else {
                    Log.i(TAG, "WIFI_p2p_ENABLED");

                }
                break;

            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:                //thrown when device's general settings change?	
                Log.i(TAG, "Received device-changed-action");

                //thrown every x seconds by the scan alarm we setup during initialization


        }

    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        Log.i(TAG, "PEERS:");
        for (WifiP2pDevice device : peers.getDeviceList()) {
            Log.i(TAG, "  ->" + device.toString());
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        //here check whether we're the group owner, then create server socket if so

    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {
        Log.i(TAG, "CMGR: Group info available: " + group.toString());

    }

    public void setIsWifiP2pEnabled(boolean opt) {
        if (opt) {
            final WifiManager mWifi = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
            mWifi.setWifiEnabled(true);
            Log.w(TAG, "Wifi is ON");
        } else {
            Log.i(TAG, "Wifi is OFF");

        }
    }

    public void discover() {
        Log.i(TAG, "Starting discovery");
        mManager.discoverPeers(mChannel, this);

    }




}
