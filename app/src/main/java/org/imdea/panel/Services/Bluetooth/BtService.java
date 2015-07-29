package org.imdea.panel.Services.Bluetooth;

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
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.imdea.panel.Database.BtMessage;
import org.imdea.panel.Database.DBHelper;
import org.imdea.panel.Global;
import org.imdea.panel.LogModule;
import org.imdea.panel.MainActivity;
import org.imdea.panel.R;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class BtService extends Service {

    public static SQLiteDatabase db;
    public static volatile ArrayList<BtMessage> messages = new ArrayList<>();
    final Timer myTimer = new Timer();
    ArrayList<String> devices = new ArrayList<>();
    BluetoothAdapter mAdapter;
    Context context;
    SharedPreferences SP;
    LogModule nLog = null;
    String currentDevice = "";
    int n_connection = 0;
    boolean answering = true;
    int nodevices = 0;
    NotificationManager mNotifyMgr;
    private String TAG = "BtService";
    private boolean busy = false;
    private boolean force_keepgoing = false;
    private ConnectionManager ConnectionThread = null;
    final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {              // When discovery finds a device
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);                   // Get the BluetoothDevice object from the Intent
                device.fetchUuidsWithSdp();
                if (isNew(device.getAddress(), devices))
                    devices.add(device.getAddress());   // add the name and the MAC address of the object to the arrayAdapter
            }

            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                mAdapter.cancelDiscovery();
                if (devices.isEmpty()) {
                    Log.i(TAG, "Discovery Finished: No Devices (try " + nodevices + ")");
                    nodevices++;
                    if (nodevices == 3) {
                        resetBt();
                        nodevices = 0;
                    }
                } else {
                    nodevices = 0;
                    Log.i(TAG, "Discovery Finished: " + devices.toString());
                    /*if (ConnectionThread == null || !ConnectionThread.isAlive()) {
                        Log.i(TAG, "Starting Synchronization");
                        ConnectionThread = new ConnectionManager();
                        ConnectionThread.start();
                    }*/
                    nLog.writeToLog("PanelLog", "BLUETOOTH", devices.toString());
                    if (n_connection % 5 == 0) {
                        nLog.getWifi();
                        //nLog.getLocation();
                    }

                    if (ConnectionThread != null) ConnectionThread.cancel();
                    ConnectionThread = null;
                    ConnectionThread = new ConnectionManager();
                    ConnectionThread.start();
                }
            }

            /*
            if (BluetoothDevice.ACTION_UUID.equals(action)) {
                (BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                for (int i = 0; i < uuidExtra.length; i++) {
                    Log.v(TAG, "\n  Device: " + device.getName() + ", " + device + ", Service: " + uuidExtra[i].toString());
                }
            }*/
        }
    };
    private BtModule mBtModule = null;
    /**
     * The Handler that gets information back from the BtModule
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Global.MESSAGE_STATE_CHANGE:
                    Intent intent = new Intent("org.imdea.panel.STATUS_CHANGED");
                    switch (msg.arg1) {
                        case BtModule.STATE_CONNECTED:
                            intent.putExtra("STATUS", "Connected");
                            sendBroadcast(intent); // Now we warn the app that we received a new message
                            break;
                        case BtModule.STATE_CONNECTING:
                            intent.putExtra("STATUS", "Connecting");
                            sendBroadcast(intent); // Now we warn the app that we received a new message

                            break;
                        case BtModule.STATE_LISTEN:
                            intent.putExtra("STATUS", "Listening");
                            sendBroadcast(intent); // Now we warn the app that we received a new message

                            break;
                        case BtModule.STATE_NONE:
                            intent.putExtra("STATUS", "Idle");
                            sendBroadcast(intent); // Now we warn the app that we received a new message

                            break;
                    }
                    break;

                case Global.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);                      // construct a string from the valid bytes in the buffer
                    //Log.w(TAG, "RECEIVED: " + readMessage);

                    //parseMessages(readMessage); // We decrypt the message and insert it into the database

                    // First we parse the messages the device sent to us
                    JSONObject obj = receiveObject(readMessage);
                    ArrayList<BtMessage> new_msgs = receiveMessages(obj);
                    ArrayList<String> new_hashes = receiveHashes(obj);

                    // If we are answering, we create the answer and send it.
                    if (answering) {

                        String s = createMessage(createMessageList(currentDevice), createHashList(new_msgs));
                        send_Message(s);
                        answering = false;

                    }

                    // After all of this, we update our list withe the new messages
                    parseInfo(new_msgs, new_hashes);

                    mBtModule.stop();

                    break;
                case Global.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    currentDevice = msg.getData().getString(Global.DEVICE_NAME);
                    break;

                case Global.CONECTION_FAILED:
                    intent = new Intent("org.imdea.panel.STATUS_CHANGED");
                    intent.putExtra("STATUS", "Error");
                    sendBroadcast(intent); // Now we warn the app that we received a new message
                    break;
            }
        }
    };

    public BtService() {
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        SP = PreferenceManager.getDefaultSharedPreferences(this);

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        Global.DEVICE_ADDRESS = mAdapter.getAddress();

        // Iniciamos el modulo bluetooth en modo escucha
        mBtModule = new BtModule(context, mHandler);
        mBtModule.start();

        Global.refresh_freq = Integer.parseInt(SP.getString("sync_frequency", "60"));
        Global.max_send_n = Integer.parseInt(SP.getString("ttl_opt", "300"));

        messages = DBHelper.recoverLiveMessages(Global.db, Global.max_send_n);

        registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND)); // Don't forget to unregister during on
        registerReceiver(bReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        //registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_UUID));

        //Foreground Service notification
        //final Intent notificationIntent = new Intent(this, MainActivity.class);
        final Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle("Panel is running")
                .setContentText("Tap to open it")
                .setContentIntent(PendingIntent.getActivity(this, 0, notificationIntent, 0));
        startForeground(2357, mBuilder.build());

        nLog = new LogModule(this);

        Log.i(TAG, "Refresh Freq set to: " + Global.refresh_freq);
        Log.i(TAG, "Max Resend Number set to: " + Global.max_send_n);

        myTimer.schedule(new TimerTask() {
            public void run() {

                messages = DBHelper.recoverLiveMessages(Global.db, Global.max_send_n);
                Log.i(TAG, messages.size() + " " + String.valueOf(busy));

                if (messages != null & messages.size() > 0) {
                    if (!busy) startDiscovery();  //If it is not busy, start
                    else force_keepgoing = true;

                } else {
                    if (force_keepgoing) {                  // If it is the next iteration, restart the service
                        Log.e(TAG, "Something went wrong, restarting Bt");
                        force_keepgoing = false;
                        mBtModule.stop();
                        mBtModule = null;
                        mAdapter.disable();
                        mAdapter.enable();
                        ConnectionThread = null;
                        mBtModule = new BtModule(context, mHandler);
                        mBtModule.start();
                    }
                }

            }

        }, 30000, Global.refresh_freq * 1000);

        return Service.START_NOT_STICKY;
    }

    public void startDiscovery() {

        if (mAdapter.isDiscovering()) mAdapter.cancelDiscovery();
        if (devices != null) devices.clear();
        mAdapter.startDiscovery();
        Log.i(TAG, "Starting Discovery");

        // This timer assures that the process finish (even if there are some strange error)

        final Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            public void run() {
                if (mAdapter.isDiscovering()) {
                    Log.i(TAG, "Discovering Time Exceeded");

                    resetBt();
                    //startDiscovery();
                    myTimer.cancel();
                }
            }

        }, 30000);

    }

    @Override
    public boolean stopService(Intent name) {
        mAdapter.cancelDiscovery();
        mBtModule.stop();
        mAdapter.disable();
        return super.stopService(name);
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
        Log.w(TAG, "Resetting Done");
    }

    public void showNotification(String title, String msg) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle("Message: " + title)
                .setContentText(msg);


        if (SP.getBoolean("notifications_new_message_vibrate", false))
            mBuilder.setVibrate(new long[]{500, 500, 500});
        mBuilder.setSound(Uri.parse(SP.getString("notifications_new_message_ringtone", "RingtoneManager.TYPE_NOTIFICATION")));

        //mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (SP.getBoolean("notifications_new_message", false))
            mNotifyMgr.notify(new Random().nextInt(100), mBuilder.build());
    }


    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Check if the new String is already stored on the arraylist
     */

    public boolean isNew(String new_item, ArrayList<String> list) {
        if (list == null || list.isEmpty()) return true;
        for (String list_item : list) {
            if (new_item.equals(list_item)) return false;
        }
        return true;
    }

    public boolean msg_isNew(ArrayList<BtMessage> list, BtMessage item) {
        for (BtMessage listitem : list) {
            if (item.toString().equals(listitem.toString())) {
                return false;
            }
        }
        return true;
    }

    private void send_Message(String message) {
        //Log.w(TAG, "SEND: " + message);
        if (message.length() > 0) {         // Check that there's actually something to send
            byte[] send = message.getBytes();     // Get the message bytes and tell the BtModule to write
            mBtModule.write(send);
        }
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

    public synchronized void updateMessage(BtMessage item) {
        for (BtMessage msg : messages) {
            if (item.toHash().equals(msg.toHash())) {
                msg.addDevice(item.last_mac_address);
                msg.addDevice(item.origin_mac_address);
            }
        }
    }

    public ArrayList receiveMessages(JSONObject json_wrapper) {
        JSONArray json_message_list;
        ArrayList<BtMessage> temporalList = new ArrayList<>();

        try {

            json_message_list = json_wrapper.getJSONArray("MESSAGES");              // Getting JSON Array node

            for (int x = 0; x < json_message_list.length(); x++) {

                JSONObject json_object = json_message_list.getJSONObject(x);
                BtMessage item = new BtMessage(json_object, currentDevice);
                Log.w(TAG, "PARSED: " + item.toString());
                item.updateHits();
                temporalList.add(item);   // Add it to the sendlist
            }
        } catch (Exception e) {
            Log.e(TAG, "Impossible to parse JsonArray Messages", e);
        }

        return temporalList;
    }

    public JSONObject receiveObject(String message) {
        JSONObject json_wrapper = null;
        try {
            json_wrapper = new JSONObject(message);
        } catch (Exception e) {
            Log.e(TAG, "Impossible to parse JSONObject", e);
        }

        return json_wrapper;
    }

    public ArrayList receiveHashes(JSONObject json_wrapper) {
        JSONArray json_message_list;
        ArrayList<String> temporalHash = new ArrayList();

        try {

            json_message_list = json_wrapper.getJSONArray("HASH");              // Getting JSON Array node

            for (int x = 0; x < json_message_list.length(); x++) {
                String hash = json_message_list.get(x).toString();
                Log.i(TAG, "HASH: " + hash);
                temporalHash.add(hash);
            }

        } catch (Exception e) {
            Log.e(TAG, "Impossible to parse JsonArray HASHES");
        }
        return temporalHash;
    }

    public void parseInfo(ArrayList<BtMessage> msgs, ArrayList<String> hashes) {

        for (BtMessage item : msgs) {

            if (addToDb(item)) {    // Check if the message is original and if it is, add it to the database
                if (msg_isNew(messages, item)) {
                    messages.add(item);   // Add it to the sendlist
                    showNotification(item.user, item.msg);                   // Notificate
                    sendBroadcast(new Intent("org.imdea.panel.MSG_RECEIVED")); // Now we warn the app that we received a new message
                }

            } else {
                updateMessage(item);
            }
        }

        BtMessage item;

        for (String hash : hashes) {
            item = DBHelper.getMessage(Global.db, hash);
            if (item != null) {
                item.addDevice(currentDevice);
                DBHelper.updateMessageDevices(Global.db, item);
            }
        }


    }

    public void parseMessages(String message) {

        JSONArray json_message_list;
        JSONObject json_wrapper = null;

        try {
            json_wrapper = new JSONObject(message);
        } catch (Exception e) {
            Log.e(TAG, "Impossible to parse JSONObject", e);
        }

        try {

            json_message_list = json_wrapper.getJSONArray("MESSAGES");              // Getting JSON Array node

            for (int x = 0; x < json_message_list.length(); x++) {

                JSONObject json_object = json_message_list.getJSONObject(x);
                BtMessage item = new BtMessage(json_object, currentDevice);
                Log.w(TAG, "PARSED: " + item.toString());
                item.updateHits();
                //Log.w(TAG, "Received: " + item.toString());

                if (addToDb(item)) {    // Check if the message is original and if it is, add it to the database
                    if (msg_isNew(messages, item)) {
                        messages.add(item);   // Add it to the sendlist
                        showNotification(item.user, item.msg);                   // Notificate
                        sendBroadcast(new Intent("org.imdea.panel.MSG_RECEIVED")); // Now we warn the app that we received a new message
                    }

                    //mHandler.obtainMessage(Global.MESSAGE_READ).sendToTarget();                       // Send the obtained bytes to the UI Activity
                    //Log.i(TAG, item.tohash());
                    //if(isNew(item.tohash(),hashmap)) hashmap.add(item.tohash());
                } else {
                    updateMessage(item);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Impossible to parse JsonArray Messages", e);
        }

        try {

            json_message_list = json_wrapper.getJSONArray("HASH");              // Getting JSON Array node

            BtMessage item;

            for (int x = 0; x < json_message_list.length(); x++) {
                String hash = json_message_list.get(x).toString();
                Log.i(TAG, "HASH: " + hash);
                item = DBHelper.getMessage(Global.db, hash);
                if (item != null) {
                    item.addDevice(currentDevice);
                    DBHelper.updateMessageDevices(Global.db, item);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Impossible to parse JsonArray HASHES");
        }


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        myTimer.cancel();
        try {
            mNotifyMgr.cancelAll();
        } catch (Exception e) {
            Log.w(TAG, "No notifications");
        }
        try {
            mBtModule.stop();
        } catch (Exception e) {
            Log.e(TAG, "ERROR", e);
        }
        try {
            unregisterReceiver(bReceiver); // Don't forget to unregister during onDestroy
        } catch (Exception e) {
            Log.e(TAG, "ERROR", e);
        }
        try {
            mHandler.removeCallbacksAndMessages(null);
        } catch (Exception e) {
            Log.e(TAG, "ERROR", e);
        }
        try {
            mAdapter.cancelDiscovery();
        } catch (Exception e) {
            Log.e(TAG, "ERROR", e);
        }
        try {
            mAdapter.cancelDiscovery();
        } catch (Exception e) {
            Log.e(TAG, "ERROR", e);
        }


    }

    public String createMessage(JSONArray msg_payload, JSONArray hash_payload) {
        JSONObject item = new JSONObject();
        try {
            item.put("MESSAGES", msg_payload);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            item.put("HASH", hash_payload);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return item.toString();
    }

    /*
    Creates a JSONArray with the specific messages that has to be sent to each device, excluding:
        Messages sent by the own device
        Messages I know thta device has
 */
    public JSONArray createMessageList(String device) {

        JSONArray my_array = new JSONArray();

        for (BtMessage message : messages) {
            if (!message.isAlreadySent(device)) {  // If the device is not on the send list
                Log.w(TAG, "SEND " + message.toJson());
                message.updateHits();
                my_array.put(message.toJson());
            }

        }

        return my_array;
    }

    public JSONArray createHashList(ArrayList<BtMessage> t_msgs) {

        JSONArray my_array = new JSONArray();

        for (BtMessage message : t_msgs) {
            my_array.put(message.toHash());

        }
        return my_array;
    }

    public JSONArray createHashList(String device) {

        JSONArray my_array = new JSONArray();

        for (BtMessage message : messages) {
            if (message.origin_mac_address.equals(device)) {
                Log.w(TAG, "SEND " + message.toHash());
                my_array.put(message.toHash());
            } else if (message.last_mac_address.equals(device)) {
                Log.w(TAG, "SEND " + message.toHash());
                my_array.put(message.toHash());
            }

        }
        return my_array;
    }

    private class ConnectionManager extends Thread {

        public ConnectionManager() {
            Log.i(TAG, "Starting ConnectionManager");
        }
        public void run() {

            busy = true;

            mAdapter = BluetoothAdapter.getDefaultAdapter();

            mBtModule.stopListening();

            if (mBtModule == null) {
                mBtModule = new BtModule(context, mHandler);
                mBtModule.start();
            }

            for (String device_addr : devices) {
                n_connection++;
                Log.w(TAG, "Connection number: " + n_connection);
                long starttime = System.currentTimeMillis();

                //1- CONNECT
                BluetoothDevice device = mAdapter.getRemoteDevice(device_addr);

                try {
                    Log.i(TAG, "Waiting for Connection " + mBtModule.getState());
                    mBtModule.connect(device);
                } catch (Exception e) {
                    Log.w(TAG, "Error while stablishing connection ", e);
                    mBtModule.setState(Global.CONECTION_FAILED);

                }

                //2-SEND MESSAGES
                long startTime = System.currentTimeMillis();

                while (mBtModule.getState() < BtModule.STATE_CONNECTED) {        //Si(MODULO NO ESTA CONECTADO o MODULO NO TIENE ERROR) --> Sigue parado.
                    if (System.currentTimeMillis() - startTime > 3000) {
                        Log.e(TAG, "Time for stablishing connection exceeded");
                        mBtModule.setState(Global.CONECTION_FAILED);
                        break;
                    }
                }

                long connectime = System.currentTimeMillis();

                if (mBtModule.getState() != Global.CONECTION_FAILED) {    //If there is no errros stablishing the connection
                    if (messages != null) {
                        JSONArray msg_payload = createMessageList(device_addr);
                        JSONArray hash_payload = createHashList(device_addr);
                        String s = createMessage(msg_payload, hash_payload);
                        if (s != null) {
                            answering = false;
                            send_Message(s);
                            try {
                                Thread.sleep(2000);
                            } catch (Exception e) {

                            }
                            //3-RECEIVE HASHES
                            //4-BYE
                            long sendtime = System.currentTimeMillis();
                            nLog.writeTimings(currentDevice, messages.hashCode(), starttime, connectime, sendtime);

                        } else {
                            Log.w(TAG, "No message***");
                        }
                    }

                }

                if (mBtModule.getState() == Global.CONECTION_FAILED + 1) {
                    Log.w(TAG, "Restart Bluetooh...");
                    resetBt();
                }
                if (mBtModule.getState() == Global.CONECTION_FAILED) {
                    Log.w(TAG, "Restart Bluetooh...");
                    resetBt();
                }

                Log.i(TAG, "Restart for Connection...");
                mBtModule.stop();
                while (mBtModule.getState() != BtModule.STATE_NONE) {
                }

                mBtModule.start();
                while (mBtModule.getState() != BtModule.STATE_LISTEN) {
                }

            }

            this.cancel();
        }

        public void cancel() {
            busy = false;
            mAdapter.cancelDiscovery();
            ConnectionThread = null;
        }

    }
}
