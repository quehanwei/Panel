package org.imdea.panel.Services.Wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
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
        WifiP2pManager.PeerListListener  {


    final int SERVER_PORT = 4576;
    final String TAG = "WifiModule";
    WifiP2pManager mManager;
    IntentFilter mIntentFilter;
    SharedPreferences SP;
    WifiP2pManager.Channel mChannel;
    String my_MAC;
    Context context;
    private List Listpeers = new ArrayList();


    WifiModule(Context context) {
        this.context = context;
    }

    public void start() {

        Log.i(TAG, "Starting Wifi Module");
        //setIsWifiP2pEnabled(true);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);

        mManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(context, context.getMainLooper(), this);

        LocalBroadcastManager.getInstance(context).registerReceiver(this, new IntentFilter("START_WIFI_SCAN"));

        context.registerReceiver(this, mIntentFilter);

        SP = PreferenceManager.getDefaultSharedPreferences(context);

        mManager.discoverPeers(mChannel, this);
    }

    @Override
    public void onChannelDisconnected() {
        Log.w(TAG, "Channel disconnected ");

    }

    @Override
    public void onFailure(int reason) {
        String text = "Scanning Failure: ";
        switch (reason) {
            case WifiP2pManager.P2P_UNSUPPORTED:
                text = text + " unsupported";
                break;
            case WifiP2pManager.BUSY:
                text = text + " busy";
                break;
            case WifiP2pManager.ERROR:
                text = text + " error";
                mManager.discoverPeers(mChannel, this);
                break;
        }

        Log.e(TAG, text);
    }

    @Override
    public void onSuccess() {
        Log.i(TAG, "Scanning started Successfully");
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        Log.i(TAG, "PEERS:");

        Listpeers.clear();
        Listpeers.addAll(peers.getDeviceList());

        if (Listpeers.size() == 0) {
            Log.d(TAG, "No devices found");
            mManager.discoverPeers(mChannel, this);

            return;
        } else {
            for (Object device : Listpeers) {
                Log.i(TAG, "  ->" + device.toString());
            }
        }

    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        //here check whether we're the group owner, then create server socket if so
        Log.i(TAG, "ConnectionInfo: " + info.toString());

        // InetAddress from WifiP2pInfo struct.
        String groupOwnerAddress = info.groupOwnerAddress.getHostAddress();
        Log.i(TAG, "IP: " + groupOwnerAddress);

        // After the group negotiation, we can determine the group owner.
        if (info.groupFormed && info.isGroupOwner) {
            // Do whatever tasks are specific to the group owner.
            // One common case is creating a server thread and accepting
            // incoming connections.
        } else if (info.groupFormed) {
            // The other device acts as the client. In this case,
            // you'll want to create a client thread that connects to the group
            // owner.
        }

    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {
        Log.i(TAG, "Group info available: " + group.toString());

    }

    /*
    public void setIsWifiP2pEnabled(boolean opt) {
        if (opt) {
            final WifiPManager mWifi = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
            mWifi.setWifiEnabled(true);
            Log.w(TAG, "Wifi is ON");
        } else {
            Log.i(TAG, "Wifi is OFF");

        }
    }*/

    public void discover() {
        Log.i(TAG, "Starting discovery");
        mManager.discoverPeers(mChannel, this);

    }

    @Override
    //this is the method that gets called when a broadcast intent is thrown
    //by the android framework that we registered for during initialization
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        switch(action){
            case WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION:
                int discoveryState = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, 0);
                switch (discoveryState) {
                    case WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED:
                        Log.w(TAG, "DISCOVERY STATE STARTED");
                        break;
                    case WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED:
                        Log.w(TAG, "DISCOVERY STATE STOPPED");
                        mManager.requestPeers(mChannel, this);
                        break;
                }
                break;

            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:          //thrown after a success full discoverPeers call
                //get the peer list via the onPeersChanged callback
                Log.i(TAG, "PeerList changed.");
                // Call WifiP2pManager.requestPeers() to get a list of current peers
                if (mManager != null)
                    mManager.requestPeers(mChannel, this);
                break;

            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:             //thrown when a connection is established/broken

                Log.i(TAG, "Connection changed");

                //grab the network info object and check if a connection was established
                NetworkInfo networkinfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                Log.i(TAG, networkinfo.toString());
                if (networkinfo.isConnected()) {
                    Log.w(TAG, "Device Connected!");
                    // We are connected with the other device, request connection
                    // info to find group owner IP

                    mManager.requestConnectionInfo(mChannel, this);
                }else {
                    LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("START_WIFI_SCAN"));
                }

                if (mManager == null) {
                    return;
                }

                break;

            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:              //this is thrown when the wifi direct state changes on the device

                //Log.i(TAG,"Received state-changed-action");

                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_DISABLED) {
                    Log.i(TAG, "WIFI_p2p_DISABLED");

                    //setIsWifiP2pEnabled(true);
                } else {
                    Log.i(TAG, "WIFI_p2p_ENABLED");

                }

                break;

            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:                //thrown when device's general settings change?
                Log.i(TAG, "Own config changed");
                WifiP2pDevice this_device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                if (this_device != null) Log.i(TAG, "Status: " + this_device.toString());
                //thrown every x seconds by the scan alarm we setup during initialization
        }

    }



}
