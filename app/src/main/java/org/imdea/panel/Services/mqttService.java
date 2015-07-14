package org.imdea.panel.Services;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.imdea.panel.Database.BtMessage;
import org.imdea.panel.Database.DBHelper;
import org.imdea.panel.Global;
import org.imdea.panel.MainActivity;
import org.imdea.panel.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/*

The user is suscribed to HIS MAC
here he receives: Messages, Acks and hellos
send info to LOG/HISMAC
send info to DEV/OTHERMACS
 */
public class mqttService extends Service implements MqttCallback {


    public static SQLiteDatabase db;
    public static volatile ArrayList<BtMessage> messages = new ArrayList<>();

    public static ArrayList<String> devices = new ArrayList<>();
    static MqttAsyncClient client;
    BluetoothAdapter mAdapter;
    SharedPreferences SP;
    int n_connection = 0;
    int nodevices = 0;
    private String TAG = "MqttService";
    private boolean busy = false;
    final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {              // When discovery finds a device
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);                   // Get the BluetoothDevice object from the Intent
                if (isNew(device.getAddress(), Global.devices))
                    devices.add(device.getAddress());   // add the name and the MAC address of the object to the arrayAdapter
            }

            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                mAdapter.cancelDiscovery();
                if (devices.isEmpty()) {

                    nodevices++;
                    if (nodevices == 3) resetBt();
                } else {
                    Global.devices = devices;
                    nodevices = 0;
                    Log.i(TAG, "Discovery Finished: " + Global.devices.toString());

                    sendLog("BLUETOOTH", Global.devices.toString());
                    if (n_connection % 5 == 0) {
                        sendLog("BATTERY", String.valueOf(getBatteryLevel()));
                        //nLog.getWifi();
                        //nLog.getLocation();
                    }

                    for (String device : getNewDevices(Global.devices, Global.old_devices)) {
                        sendHandsake(device);
                    }
                }
                Intent intent2 = new Intent("org.imdea.panel.STATUS_CHANGED");
                intent2.putExtra("STATUS", Global.devices.size() + " devices");
                sendBroadcast(intent2); // Now we warn the app that we received a new message
            }
        }
    };
    private MQTTConnectionStatus connectionStatus = MQTTConnectionStatus.INITIAL;

    public static void sendToPeers(BtMessage msg) {
        MqttMessage mqtt_msg = new MqttMessage(msg.toJson().toString().getBytes());
        mqtt_msg.setQos(2);
        for (String device : Global.devices) {
            IMqttToken token;
            try {
                token = client.publish("dev/" + device, mqtt_msg);
                token.waitForCompletion(5000);

            } catch (MqttException e) {
                Log.e("MqttService", "Error sending Message", e);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void showNotification(String title, String msg) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle("Message: " + title)
                .setContentText(msg);


        if (SP.getBoolean("notifications_new_message_vibrate", false))
            mBuilder.setVibrate(new long[]{500, 500, 500});

        mBuilder.setSound(Uri.parse(SP.getString("notifications_new_message_ringtone", "RingtoneManager.TYPE_NOTIFICATION")));

        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (SP.getBoolean("notifications_new_message", true))
            mNotifyMgr.notify(87, mBuilder.build());
        //new Random().nextInt(100)
    }

    public int getBattery() {
        return 0;
    }

    public String[] getNewDevices(ArrayList<String> newlist, ArrayList<String> oldlist) {

        // If the mac is in both lists, delete the mac.
        //Later, return the remaining macs that would be the new ones
        for (String newmac : newlist) {
            for (String oldmac : oldlist) {
                if (newmac.equals(oldmac)) newlist.remove(newmac);
            }
        }

        return newlist.toArray(new String[newlist.size()]);
    }


    public String[] getOldDevices(ArrayList<String> newlist, ArrayList<String> oldlist) {

        // We iterate over the old list
        // if there is a coincidence, do nothing
        // if one of the old lsit is not in the new one, save it
        boolean isNew = true;
        ArrayList<String> result = new ArrayList<>();
        for (String oldmac : oldlist) {
            for (String newmac : newlist) {
                if (newmac.equals(oldmac)) isNew = false;
            }
            if (isNew) result.add(oldmac);
            isNew = true;
        }

        return result.toArray(new String[result.size()]);

    }


    public boolean isNew(String new_item, ArrayList<String> list) {
        if (list == null || list.isEmpty()) return true;
        for (String list_item : list) {
            if (new_item.equals(list_item)) return false;
        }
        return true;
    }

    public void resetBt() {
        Log.w(TAG, "Resetting Bt");
        busy = true;
        mAdapter.cancelDiscovery();
        while (mAdapter.isDiscovering()) {

        }
        mAdapter.disable();
        while (mAdapter.isEnabled()) {

        }
        mAdapter.enable();
        while (!mAdapter.isEnabled()) {

        }
        busy = false;
    }

    public void startDiscovery() {

        if (mAdapter.isDiscovering()) mAdapter.cancelDiscovery();
        //if (Global.devices != null) Global.devices.clear();
        mAdapter.startDiscovery();
        Log.i(TAG, "Starting Discovery");
        Intent intent2 = new Intent("org.imdea.panel.STATUS_CHANGED");
        intent2.putExtra("STATUS", "Discovering");
        sendBroadcast(intent2); // Now we warn the app that we received a new message
        // This timer assures that the process finish (even if there are some strange error)

        final Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            public void run() {
                if (mAdapter.isDiscovering()) {
                    Log.e(TAG, "Discovering Time Exceeded");

                    resetBt();
                    //startDiscovery();
                    myTimer.cancel();
                }
            }

        }, 30000);

    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        SP = PreferenceManager.getDefaultSharedPreferences(this);

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        Global.DEVICE_ADDRESS = mAdapter.getAddress();

        Global.refresh_freq = Integer.parseInt(SP.getString("sync_frequency", "60"));
        Global.max_send_n = Integer.parseInt(SP.getString("ttl_opt", "300"));

        messages = DBHelper.recoverLiveMessages(Global.db, Global.max_send_n);

        registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND)); // Don't forget to unregister during onDestroy
        registerReceiver(bReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

        //Foreground Service notification
        final Intent notificationIntent = new Intent(this, MainActivity.class);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle("Panel is running")
                .setContentText("Tap to open it")
                .setContentIntent(PendingIntent.getActivity(this, 0, notificationIntent, 0));
        startForeground(2357, mBuilder.build());

        //nLog = new LogModule(this);
        Log.i(TAG, "Refresh Freq set to: " + Global.refresh_freq);
        Log.i(TAG, "Max Resend Number set to: " + Global.max_send_n);

        start_connection();

        final Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            public void run() {

                messages = DBHelper.recoverLiveMessages(Global.db, Global.max_send_n);

                Log.i(TAG, "MESG: " + messages.size() + " " + String.valueOf(busy));

                /*
                if (messages != null & messages.size() > 0) {
                    if (!mAdapter.isDiscovering()) startDiscovery();  //If it is not busy, start
                }*/

                if (!mAdapter.isDiscovering()) startDiscovery();  //If it is not busy, start

            }

        }, 30000, Global.refresh_freq * 1000);

        return Service.START_NOT_STICKY;
    }

    public synchronized boolean addToDb(BtMessage item) {

        Boolean Success = false;
        DBHelper msg_database = new DBHelper(getApplicationContext(), "messages.db", null, 1);
        db = msg_database.getWritableDatabase();

        if (!item.isGeneral) {          // If It is a Tag
            if (DBHelper.existTag(db, item.tag)) if (DBHelper.isNew(db, item)) {
                DBHelper.insertMessage(db, item);
                Success = true;
            } else DBHelper.updateMessageDevices(db, item);
        } else {
            if (DBHelper.isNew(db, item)) DBHelper.insertMessage(db, item);
            else DBHelper.updateMessageDevices(db, item);
            Success = true;
        }

        db.close();
        return Success;
    }

    public void sendHandsake(String macaddr) {

        MqttMessage mqtt_msg = new MqttMessage(("X" + Global.DEVICE_ADDRESS).getBytes());
        mqtt_msg.setQos(2);

        IMqttToken token;
        try {
            token = client.publish("dev/" + macaddr, mqtt_msg);
            token.waitForCompletion(5000);

        } catch (Exception e) {
            Log.e(TAG, "Error sending Hello", e);
        }
    }

    public void sendAck(BtMessage item) {

        MqttMessage mqtt_msg = new MqttMessage((Global.DEVICE_ADDRESS + item.toHash()).getBytes());
        mqtt_msg.setQos(2);

        IMqttToken token;
        try {
            token = client.publish("dev/" + item.last_mac_address, mqtt_msg);
            token.waitForCompletion(5000);

        } catch (MqttException e) {
            Log.e(TAG, "Error sending Ack", e);
        }
    }

    /**
     * connectionLost
     * This callback is invoked upon losing the MQTT connection.
     */
    @Override
    public void connectionLost(Throwable t) {
        Log.w(TAG, "Connection lost!");
        start_connection();
        // code to reconnect to the broker would go here if desired
    }

    /**
     * deliveryComplete
     * This callback is invoked when a message published by this client
     * is successfully received by the broker.
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        //Log.w(TAG,"Delivered");
    }

    /**
     * messageArrived
     * This callback is invoked when a message is received on a subscribed topic.
     */
    public void messageArrived(String topic, final MqttMessage msg) throws Exception {
        Log.i(TAG, "From topic " + topic);

        Handler h = new Handler(getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {

                String msgstring = new String(msg.getPayload());

                //Toast.makeText(getApplicationContext(), msgstring, Toast.LENGTH_LONG).show()

                //That`s a virtual discovery, when a device can see the others, some sort of handsake to wanr them
                if (msgstring.charAt(0) == 'X') {
                    Log.w(TAG, "HELLO from " + msgstring.substring(1, 18));
                    boolean isNew = true;
                    for (String device : Global.devices) {
                        if (msgstring.substring(1, 18).equals(device)) {
                            isNew = false;
                            break;
                        }

                    }
                    if (isNew) Global.devices.add(msgstring.substring(1, 18));

                    for (String device : devices) {
                        if (msgstring.substring(1, 18).equals(device)) {
                            isNew = false;
                            break;
                        }

                    }
                    if (isNew) devices.add(msgstring.substring(1, 18));

                } else if (!msgstring.contains("{")) {
                    String hash = msgstring.substring(17);
                    String macAddr = msgstring.substring(0, 17);
                    Log.w(TAG, "ACK from " + macAddr + " " + hash);

                    for (BtMessage msg : messages) {
                        if (hash.equals(msg.toHash())) {
                            msg.addDevice(macAddr);
                            DBHelper.updateMessageDevices(Global.db, msg);
                        }
                    }


                } else {
                    Log.w(TAG, "MSG: " + msgstring);
                    BtMessage item = new BtMessage(msgstring);
                    showNotification(item.user, item.msg);
                    addToDb(item);
                    sendBroadcast(new Intent("org.imdea.panel.MSG_RECEIVED")); // Now we warn the app that we received a new message
                    sendAck(item);

                }


            }
        });
    }

    public void start_connection() {
        IMqttToken token;
        boolean succesful = true;

        try {
            String url = String.format(Locale.US, "tcp://%s:%d", Global.SERVER_IP, Global.SERVER_PORT);
            client = new MqttAsyncClient(url, Global.DEVICE_ADDRESS, new MemoryPersistence());
            token = client.connect();
            token.waitForCompletion(3500);
            client.setCallback(this);
        } catch (Exception e) {
            Log.e(TAG, "Error Creating Client", e);
            Intent intent = new Intent("org.imdea.panel.STATUS_CHANGED");
            sendBroadcast(intent.putExtra("STATUS", "Disconnected")); // Now we warn the app that we received a new message
            return;
        }

        try {
            Log.i(TAG, "Suscribing to DEV/" + Global.DEVICE_ADDRESS);
            token = client.subscribe("dev/" + Global.DEVICE_ADDRESS, 2);
            token.waitForCompletion(5000);

        } catch (Exception e) {
            Log.e(TAG, " Impossible to suscribe to this DEV Group");
        }
    }

    public void sendLog(String title, String logtext) {
        String finalstring = new SimpleDateFormat("yyyy.MM.dd.HH:mm:ss").format(Calendar.getInstance().getTime()) + "\t" + Global.DEVICE_ADDRESS + "\t" + title + "\t" + logtext + "\n";
        MqttMessage mqtt_msg = new MqttMessage(finalstring.getBytes());
        mqtt_msg.setQos(2);

        IMqttToken token;
        try {
            token = client.publish("log/" + Global.DEVICE_ADDRESS, mqtt_msg);
            token.waitForCompletion(5000);

        } catch (MqttException e) {
            Log.e(TAG, "Error Sending ACK", e);
        }
    }

    public float getBatteryLevel() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        // Error checking that probably isn't needed but I added just in case.
        if (level == -1 || scale == -1) {
            return 50.0f;
        }

        return ((float) level / (float) scale) * 100.0f;
    }

    public void getWifi() {
        final WifiManager mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        if (mWifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
            mWifiManager.setWifiEnabled(true);
        }

        // register WiFi scan results receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                List<ScanResult> results = mWifiManager.getScanResults();
                String s = "";
                for (ScanResult ap : results) {
                    s = s + ap.SSID + "," + ap.BSSID + "\t";
                }
                try {
                    unregisterReceiver(this);
                } catch (Exception e) {
                    Log.e(TAG, "Error unregistering");
                }
                sendLog("WIFI", s);
            }
        }, filter);

        // start WiFi Scan
        mWifiManager.startScan();


    }

    public enum MQTTConnectionStatus {
        INITIAL,                            // initial status
        CONNECTING,                         // attempting to connect
        CONNECTED,                          // connected
        NOTCONNECTED_WAITINGFORINTERNET,    // can't connect because the phone
        //     does not have Internet access
        NOTCONNECTED_USERDISCONNECT,        // user has explicitly requested
        //     disconnection
        NOTCONNECTED_DATADISABLED,          // can't connect because the user
        //     has disabled data access
        NOTCONNECTED_UNKNOWNREASON          // failed to connect for some reason
    }
}

