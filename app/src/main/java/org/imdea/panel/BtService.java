package org.imdea.panel;

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

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/* TODO
        -> Send message confirmation
        -> Assure that the same message is not send to the same device twice
        -> Fix the Outdated error

 */
public class BtService extends Service {

    public static SQLiteDatabase db;

    ArrayList<String> devices = new ArrayList<>();
    ArrayList<String> hashmap = new ArrayList<>();

    BluetoothAdapter mAdapter;
    Context context;
    SharedPreferences SP;
    private String TAG = "BtService";
    /**
     * The Handler that gets information back from the BtModule
     */
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
                    parseJson(readMessage); // We decrypt the message and insert it into the database
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
    private BtModule mChatService = null;
    private boolean busy = false;
    private boolean force_keepgoing = false;
    private ConnectionManager ConnectionThread = null;
    final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {              // When discovery finds a device
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);                   // Get the BluetoothDevice object from the Intent
                if (isNew(device.getAddress(), devices))
                    devices.add(device.getAddress());   // add the name and the MAC address of the object to the arrayAdapter
            }
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (devices.isEmpty()) {
                    Log.i(TAG, "Discovery Finished: No Devices");
                    mAdapter.cancelDiscovery();
                    mAdapter.disable();
                    mAdapter.enable();
                } else {
                    Log.i(TAG, "Discovery Finished: " + devices.toString());
                    /*if (ConnectionThread == null || !ConnectionThread.isAlive()) {
                        Log.i(TAG, "Starting Synchronization");
                        ConnectionThread = new ConnectionManager();
                        ConnectionThread.start();
                    }*/
                    if (ConnectionThread != null) ConnectionThread.cancel();
                    ConnectionThread = null;
                    Log.i(TAG, "Starting Synchronization");
                    ConnectionThread = new ConnectionManager();
                    ConnectionThread.start();
                }
                mAdapter.cancelDiscovery();

            }
        }
    };

    public BtService() {
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        SP = PreferenceManager.getDefaultSharedPreferences(this);

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mChatService = new BtModule(context, mHandler);
        mChatService.start();

        registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND)); // Don't forget to unregister during onDestroy
        registerReceiver(bReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

        final Intent notificationIntent = new Intent(this, MainActivity.class);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle("Panel running")
                .setContentIntent(PendingIntent.getActivity(this, 0, notificationIntent, 0));
        startForeground(2357, mBuilder.build());

        final Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            public void run() {

                //Clean outdated messages
                //CleanOutdated();
                // If the queue is not empty AND there is some message pending AND the system is not busy

                Log.i(TAG, Global.messages + " " + Global.messages.size() + " " + String.valueOf(busy));

                if (Global.messages != null & Global.messages.size() > 0) {
                    if (!busy) startDiscovery();  //If it is not busy, start
                    else force_keepgoing = true;

                } else {
                    if (force_keepgoing) {                  // If it is the next iteration, restart the service
                        Log.e(TAG, "Something went wrong, restarting Bt");
                        force_keepgoing = false;
                        mChatService.stop();
                        mAdapter.disable();
                        mAdapter.enable();
                        ConnectionThread = null;
                        mChatService.start();
                    }
                }

            }

        }, 30000, 90000);

        return Service.START_NOT_STICKY;
    }

    public synchronized void CleanOutdated() {
        if (Global.messages != null) for (BtMessage obj : Global.messages) {
            Log.i(TAG, String.valueOf(checkTime(obj.time)));
            if (checkTime(obj.time) > 10) removeFromQueue(obj);
        }
    }

    public long checkTime(String time) {
        try {
            Date then = new SimpleDateFormat("HH:mm").parse(time);
            Date now = new Date();
            Long diff = now.getTime() - then.getTime();
            return (int) ((diff / (1000 * 60)) % 60);
        } catch (Exception e) {

        }
        return 0;
    }

    public synchronized void removeFromQueue(BtMessage itm) {
        Global.messages.remove(itm);
    }

    public void showNotification(String title, String msg) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle("Message: " + title)
                .setContentText(msg);


        if (SP.getBoolean("notifications_new_message_vibrate", false))
            mBuilder.setVibrate(new long[]{500});
        mBuilder.setSound(Uri.parse(SP.getString("notifications_new_message_ringtone", "RingtoneManager.TYPE_NOTIFICATION")));

        //mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (SP.getBoolean("notifications_new_message", false))
            mNotifyMgr.notify(new Random().nextInt(100), mBuilder.build());
    }

    public void startDiscovery() {
        if (mAdapter.isDiscovering()) mAdapter.cancelDiscovery();
        if (devices != null) devices.clear();
        //if (isEmpty()) return;
        mAdapter.startDiscovery();
        Log.i(TAG, "Starting Discovery");
        final Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            public void run() {
                if (mAdapter.isDiscovering()) {
                    Log.i(TAG, "Discovering Time Exceeded");
                    mAdapter.cancelDiscovery();
                    mAdapter.disable();
                    mAdapter.enable();
                    startDiscovery();
                    myTimer.cancel();
                }
            }

        }, 30000);

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
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
            if (item.toString().equals(listitem.toString())) return false;
        }
        return true;
    }

    private JSONArray prepareHashes() {

        JSONArray my_array = new JSONArray();
        for (String hash : hashmap) {
            my_array.put(hash);
        }
        return my_array;

    }

    private String getHash(BtMessage item) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("HASH", item.tohash());
        } catch (Exception e) {

        }
        return obj.toString();
    }

    private String prepareMessage() {

        JSONArray my_array = new JSONArray();
        for (BtMessage message : Global.messages) {
            my_array.put(message.toJson());
        }
        JSONObject item = new JSONObject();
        try {
            item.put("MESSAGES", my_array);
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*try {
           item.put("HASH", prepareHashes());
        }catch(Exception e){

        }*/
        return item.toString();
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BtModule to write
            byte[] send = message.getBytes();
            mChatService.write(send);
        }
    }

    public boolean addToDb(BtMessage item) {
        Boolean Success = false;
        DBHelper msg_database = new DBHelper(getApplicationContext(), "messages.db", null, 1);
        db = msg_database.getWritableDatabase();
        if (!item.isGeneral) {
            if (DBHelper.TagExists(db, item.tag)) if (DBHelper.isNew(db, item)) {
                DBHelper.insertMessage(db, item);
                Success = true;
            }
        } else {
            if (DBHelper.isNew(db, item)) DBHelper.insertMessage(db, item);
            Success = true;
        }
        db.close();
        return Success;
    }

    public synchronized void addToList(BtMessage item) {
        Global.messages.add(item);
    }

    public void parseJson(String message) {
        //Log.w("RECEIVED: ", message);

        JSONArray json_list;
        JSONObject json_wrapper;
        try {
            json_wrapper = new JSONObject(message);
            json_list = json_wrapper.getJSONArray("MESSAGES");              // Getting JSON Array node
            for (int x = 0; x < json_list.length(); x++) {
                JSONObject json_object = json_list.getJSONObject(x);
                BtMessage item = new BtMessage(json_object);
                item.updateHits();
                Log.w(TAG, "Received: " + item.toString());
                if (addToDb(item)) {    // Check if the message is original and if it is, add it to the database
                    if (msg_isNew(Global.messages, item))
                        addMessage(item);   // Add it to the sendlist
                    showNotification(item.user, item.msg);                   // Notificate

                    //mHandler.obtainMessage(Global.MESSAGE_READ).sendToTarget();                       // Send the obtained bytes to the UI Activity
                    //Log.i(TAG, item.tohash());
                    //if(isNew(item.tohash(),hashmap)) hashmap.add(item.tohash());
                }
            }

            /*json_list = json_wrapper.getJSONArray("HASH");
            for(int x=0; x<json_list.length(); x++){
                String hash = json_list.getString(x);
                for(BtMessage mesg:Global.messages){
                    if(mesg.tohash().equals(hash)) mesg.updateHits();
                }
            }*/
        } catch (Exception e) {
            Log.e(TAG, "Impossible to parse JSON", e);
        }

    }

    public void addMessage(BtMessage Item) {
        addToList(Item);
    }

    private class ConnectionManager extends Thread {

        public ConnectionManager() {
            Log.i(TAG, "Starting ConnectionManager");
        }

        public void run() {

            busy = true;
            for (String device_addr : devices) {
                //1- CONNECT
                BluetoothDevice device = mAdapter.getRemoteDevice(device_addr);
                mChatService.connect(device);
                //2-SEND MESSAGES
                Log.i(TAG, "Waiting for Connection " + mChatService.getState());
                long startTime = System.currentTimeMillis();
                while (mChatService.getState() < BtModule.STATE_CONNECTED) {        //Si(MODULO NO ESTA CONECTADO o MODULO NO TIENE ERROR) --> Sigue parado.
                    //Log.i("STATUS",String.valueOf(System.currentTimeMillis() - startTime));
                    if (System.currentTimeMillis() - startTime > 5000) {
                        Log.e(TAG, "Time for stablishing connection exceeded");
                        mChatService.setState(Global.CONECTION_FAILED);
                        break;
                    }
                }

                if (mChatService.getState() != Global.CONECTION_FAILED) {    //If there is no errros stablishing the connection
                    Log.i(TAG, prepareMessage());
                    sendMessage(prepareMessage());
                    //3-RECEIVE HASHES
                    //4-BYE
                    Log.i(TAG, "Waiting for Server restart.");
                    mChatService.start();
                    while (mChatService.getState() != BtModule.STATE_LISTEN) {
                    }
                }
            }
            busy = false;
        }

        public void cancel() {
            busy = false;
            mChatService.stop();
            mChatService = null;
            ConnectionThread = null;
        }

    }
}
