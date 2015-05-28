package org.imdea.panel.Wireless;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class WifiService extends Service {

    public static List peers = new ArrayList();
    final String SERVER_PORT = "4576";
    final String TAG = "WifiService";
    final HashMap<String, String> buddies = new HashMap<String, String>();
    WifiP2pManager mManager;
    IntentFilter mIntentFilter;
    WiFiDirectBroadcastReceiver Receiver;
    SharedPreferences SP;
    WifiP2pManager.Channel mChannel;
    boolean isConnected = false;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Do your stuff here
            ScanWifi();
        }
    };

    public WifiService() {
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "Starting Wifi Service");

        setIsWifiP2pEnabled(true);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        Receiver = new WiFiDirectBroadcastReceiver(mManager, mChannel);

        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, new IntentFilter("START_WIFI_SCAN"));
        registerReceiver(Receiver, mIntentFilter);

        SP = PreferenceManager.getDefaultSharedPreferences(this);

        startRegistration();

        /*
        final Timer myTimer = new Timer();
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

    public void setIsWifiP2pEnabled(boolean opt) {
        if (opt) {
            final WifiManager mWifi = (WifiManager) getSystemService(WIFI_SERVICE);
            mWifi.setWifiEnabled(true);
            Log.i(TAG, "Wifi is ON");
        } else {
            Log.i(TAG, "Wifi is OFF");

        }
    }

    private void startRegistration() {

        Log.i(TAG, "Registering Service");
        //  Create a string map containing information about your service.
        Map record = new HashMap();
        record.put("listenport", String.valueOf(SERVER_PORT));
        record.put("username", SP.getString("username", "Anonymous"));
        record.put("available", "visible");

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("FloatyChat", "_presence._tcp", record);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        mManager.addLocalService(mChannel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Command successful! Code isn't necessarily needed here,
                // Unless you want to update the UI or add logging statements.
                Log.i(TAG, "Service Succesfully registered");
            }

            @Override
            public void onFailure(int arg0) {
                Log.e(TAG, "Registration failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY");

                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
            }
        });
    }

    public void ScanWifi() {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {

                Log.i(TAG, "Wifi Scanning initiated Succefully");
                // Code for when the discovery initiation is successful goes here.
                // No services have actually been discovered yet, so this method
                // can often be left blank.  Code for peer discovery goes in the
                // onReceive method, detailed below.
            }

            @Override
            public void onFailure(int reasonCode) {

                Log.e(TAG, "Error initiating WifiScan");
                // Code for when the discovery initiation fails goes here.
                // Alert the user that something went wrong.
            }
        });
    }

    private void discoverService() {

        WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
        /* Callback includes:
         * fullDomain: full domain name: e.g "printer._ipp._tcp.local."
         * record: TXT record dta as a map of key/value pairs.
         * device: The device running the advertised service.
         */
            public void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {
                Log.d(TAG, "DnsSdTxtRecord available -" + record.toString());
                buddies.put(device.deviceAddress, record.get("username").toString());
            }
        };

        WifiP2pManager.DnsSdServiceResponseListener servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice resourceType) {

                // Update the device name with the human-friendly version from
                // the DnsTxtRecord, assuming one arrived.
                resourceType.deviceName = buddies.containsKey(resourceType.deviceAddress) ? buddies.get(resourceType.deviceAddress) : resourceType.deviceName;

                Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
            }
        };

        mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);
    }

    public void connect() {
        // Picking the first device found on the network.
        WifiP2pDevice device = (WifiP2pDevice) peers.get(0);

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
                Log.i(TAG, "Connection failed");
            }
        });
    }
}

