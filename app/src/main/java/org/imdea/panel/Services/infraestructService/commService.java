package org.imdea.panel.Services.infraestructService;

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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.layer.sdk.LayerClient;
import com.layer.sdk.changes.LayerChange;
import com.layer.sdk.changes.LayerChangeEvent;
import com.layer.sdk.exceptions.LayerException;
import com.layer.sdk.listeners.LayerAuthenticationListener;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.layer.sdk.listeners.LayerConnectionListener;
import com.layer.sdk.listeners.LayerProgressListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.ConversationOptions;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;
import com.layer.sdk.query.Predicate;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.SortDescriptor;
import com.parse.FunctionCallback;
import com.parse.ParseCrashReporting;
import com.parse.Parse;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;

import org.imdea.panel.Database.BtMessage;
import org.imdea.panel.Database.DBHelper;
import org.imdea.panel.Database.Messages;
import org.imdea.panel.Global;
import org.imdea.panel.MainActivity;
import org.imdea.panel.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/*
Provider ID             layer:///providers/cd952880-369f-11e5-89ac-fdeb230102e2
Authentication key ID   layer:///keys/7d72b384-36e2-11e5-8c96-fdeb22010ca4
 */
@SuppressWarnings({"unchecked", "deprecation"})

public class commService extends Service implements LayerChangeEventListener.BackgroundThread, LayerConnectionListener, LayerAuthenticationListener  {

    private static final String TAG = "commService";
    public static SQLiteDatabase db;

    public static volatile ArrayList<BtMessage> messages = new ArrayList<>();
    public static volatile ArrayList<Conversation> generalConversation = new ArrayList<>();

    public static ArrayList<String> devices = new ArrayList<>();
    BluetoothAdapter mAdapter;
    SharedPreferences SP;
    int n_connection = 0;
    int nodevices = 0;
    private boolean busy = false;
    public LayerClient layerClient;
    Timer myTimer, logTimer;

    public final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {              // When discovery finds a device
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);                   // Get the BluetoothDevice object from the Intent
                if (isNew(device.getAddress(), Global.devices))
                    devices.add(device.getAddress());   // add the name and the MAC address of the object to the arrayAdapter
            }

            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

                mAdapter.cancelDiscovery();
                //if (client.isConnected()) connectionStatus = MQTTConnectionStatus.CONNECTED;
                if (devices.isEmpty()) {
                    Log.i(TAG, "Discovery Finished: 0");
                    nodevices++;
                    if (nodevices == 5) resetBt();
                } else {
                    Global.devices = devices;
                    nodevices = 0;
                    Log.i(TAG, "Discovery Finished: " + Global.devices.toString());
                    if(n_connection % 5 == 0){
                        ParseObject gameScore = new ParseObject("Bluetooth");
                        gameScore.put("MAC", Global.DEVICE_ADDRESS);
                        gameScore.put("date", new SimpleDateFormat("yyyy.MM.dd").format(Calendar.getInstance().getTime()));
                        gameScore.put("time", new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()));
                        gameScore.addAllUnique("Devices", Global.devices);
                        gameScore.saveEventually();
                    }

                    n_connection++;

                    /*for (String device : getNewDevices(Global.devices, Global.old_devices)) {
                        sendHandsake(device);
                    }*/

                    //if (!failedMessages.isEmpty()) sendFailedMessages();
                    /*
                    try {
                        receiveByPeers();
                    }catch(Exception e){
                        Log.e(TAG,"Error receiven messages from conversation",e);
                    }*/

                    try {
                        generalConversation = null;
                        generalConversation = new ArrayList<>();
                        ConversationOptions generalConversationOptions = new ConversationOptions().distinct(false);
                        for(String device:devices){
                            Conversation DeviceConversation =  layerClient.newConversation(generalConversationOptions,device);
                            generalConversation.add(DeviceConversation);

                        }
                    }catch (Exception e){
                        Log.e(TAG,"Errror",e);
                    }
                }
                Intent intent2 = new Intent("org.imdea.panel.STATUS_CHANGED");
                intent2.putExtra("STATUS", Global.devices.size() + " devices");
                sendBroadcast(intent2); // Now we warn the app that we received a new message
            }

            if (action.equalsIgnoreCase("org.imdea.panel.MESSAGE_WRITTEN")) {
                messages = DBHelper.recoverLiveMessages(Global.db, Global.max_send_n);
                sendToPeers(messages);

            }
        }
    };


    public void sendToPeers(ArrayList<BtMessage> msgs) {
        // Sends the specified message

        if(generalConversation!=null){
            for(Conversation conversation :  generalConversation){
                String destination_device = conversation.getParticipants().get(0);
                Log.w(TAG,destination_device);
                ArrayList<MessagePart> messageparts = new ArrayList<>();
                for(BtMessage msg : msgs){
                    if(!msg.isAlreadySent(destination_device)){
                        MessagePart messagePart = layerClient.newMessagePart("text/plain", msg.toJson().toString().getBytes());
                        messageparts.add(messagePart);
                    }

                }
                final Message message = layerClient.newMessage(messageparts);
                conversation.send(message, new LayerProgressListener() {
                    public void onProgressStart(MessagePart messagePart, Operation operation) {
                    }
                    public void onProgressUpdate(MessagePart messagePart, LayerProgressListener.Operation operation, long l) {
                    }
                    public void onProgressComplete(MessagePart messagePart, Operation operation) {
                        BtMessage item = new BtMessage(messagePart.toString());
                        DBHelper.updateMessage(Global.db, item);
                    }
                    public void onProgressError(MessagePart messagePart, Operation operation, Throwable throwable) {
                    }
                });
            }
        }
    }

    public void receiveByPeers(Message message){
        /*
        Query query = Query.builder(Message.class)
                    .predicate(new Predicate(Message.Property.CONVERSATION, Predicate.Operator.EQUAL_TO, generalConversation))
                    .sortDescriptor(new SortDescriptor(Message.Property.POSITION, SortDescriptor.Order.ASCENDING))
                    .build();

        List<Message> results = layerClient.executeQuery(query, Query.ResultType.OBJECTS);*/

        //for(Message message : results){
            //String senderID = message.getSender().getUserId();        //The sender's user id
            //You will also need to check the message's Mime Type (set when the message was sent) in order to know how to decode the message contents.

            List<MessagePart> parts = message.getMessageParts();
            for(MessagePart part : parts) {
                switch (part.getMimeType()) {

                    case "text/plain":
                        String textMsg = new String(part.getData());
                        BtMessage item = new BtMessage(textMsg);
                        Log.w(TAG,"MSG "+ textMsg);
                        if(addToDb(item))
                        {
                            sendBroadcast(new Intent("org.imdea.panel.MSG_RECEIVED")); // Now we warn the app that we received a new message
                            showNotification(item.user,item.msg);
                        }
                        break;

                    case "image/jpeg":
                        //Bitmap imageMsg = BitmapFactory.decodeByteArray(part.getData(), 0, part.getData().length);
                        break;
                }
            }
        //}

    }

    /*
        SEND MESSAGES *******************************************************************************************
     */

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Initiating ");

        // Option 1: Create a standard LayerClient object
        layerClient = LayerClient.newInstance(getApplicationContext(), "layer:///apps/staging/cd96cd52-369f-11e5-bc78-fdeb230102e2");

        // Initializing parse for logging
        //if(!ParseCrashReporting.isCrashReportingEnabled())  ParseCrashReporting.enable(this);
        Parse.initialize(this, "83P6Ag7ABhxNaIWxGdBudnQlI73zFFo5ELDpgOwF", "pSLa7BTd7GPgWDyeQITZZ5rhK6gVzfKU8i69CSLU");

        layerClient.registerConnectionListener(this);
        layerClient.registerAuthenticationListener(this);
        layerClient.registerEventListener(this);


        SP = PreferenceManager.getDefaultSharedPreferences(this);

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        Global.DEVICE_ADDRESS = mAdapter.getAddress();

        Global.refresh_freq = Integer.parseInt(SP.getString("sync_frequency", "60"));
        Global.max_send_n = Integer.parseInt(SP.getString("ttl_opt", "300"));

        messages = DBHelper.recoverLiveMessages(Global.db, Global.max_send_n);

        registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND)); // Don't forget to unregister during onDestroy
        registerReceiver(bReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        registerReceiver(bReceiver, new IntentFilter("org.imdea.panel.MESSAGE_WRITTEN"));
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

        //nLog = new LogModule(this);
        Log.i(TAG, "Refresh Freq set to: " + Global.refresh_freq);
        Log.i(TAG, "Max Resend Number set to: " + Global.max_send_n);

        // Asks the LayerSDK to establish a network connection with the Layer service
        layerClient.connect();

        //DBHelper.deleteOutdated(Global.db);

        myTimer = new Timer();
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

        }, 10000, Global.refresh_freq * 1000);

        logTimer = new Timer();
        logTimer.schedule(new TimerTask() {
            public void run() {
                sendBatteryLog();
                getWifi();

            }

        }, 30000, 60000*15);

        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    public void onEventAsync(LayerChangeEvent event) {
        List<LayerChange> changes = event.getChanges();
        for(LayerChange change: changes){
            switch (change.getObjectType()) {
                case CONVERSATION:
                    switch (change.getChangeType()) {
                        case INSERT:
                            Log.w(TAG,"New Conversation");
                            break;

                        case UPDATE:
                            Log.w(TAG,"Updated Conversation");
                            break;

                        case DELETE:
                            Log.w(TAG,"Deleted Conversation");
                            break;
                    }
                    break;

                case MESSAGE:
                    Log.w(TAG,"Message changed");
                    switch (change.getChangeType()) {
                        case INSERT:

                            Log.i(TAG, "MSG Received: ");
                            receiveByPeers((Message) change.getObject());
                            break;

                        case UPDATE:
                            // Object was updated
                            break;

                        case DELETE:
                            // Object was deleted
                            break;
                    }
                    break;
            }
        }
    }

    public void sendLogMsg(BtMessage item) {
        ParseObject gameScore = new ParseObject("Message");
        gameScore.put("date", new SimpleDateFormat("yyyy.MM.dd").format(Calendar.getInstance().getTime()));
        gameScore.put("time", new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()));
        gameScore.put("From", item.origin_mac_address);
        gameScore.put("To", Global.DEVICE_ADDRESS);
        gameScore.put("Hash", item.toHash());
        gameScore.saveEventually();
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

    public boolean isNew(String new_item, ArrayList<String> list) {
        if (list == null || list.isEmpty()) return true;
        for (String list_item : list) {
            if (new_item.equals(list_item)) return false;
        }
        return true;
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



    /*public void messageArrived()  {
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

                    try {
                        BtMessage item = DBHelper.getMessage(Global.db, hash);
                        if (item != null) {
                            item.addDevice(macAddr);
                            DBHelper.updateMessage(Global.db, item);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error Updating Message");
                    }


                } else {
                    BtMessage item = new BtMessage(msgstring);
                    Log.w(TAG, "MSG from " + item.last_mac_address + " -" + item.msg);
                    showNotification(item.user, item.msg);
                    addToDb(item);
                    sendBroadcast(new Intent("org.imdea.panel.MSG_RECEIVED")); // Now we warn the app that we received a new message
                    sendAck(item);

                }


            }
        });
    }*/

    public void sendBatteryLog() {
        ParseObject gameScore = new ParseObject("Battery");
        gameScore.put("MAC", Global.DEVICE_ADDRESS);
        gameScore.put("date", new SimpleDateFormat("yyyy.MM.dd").format(Calendar.getInstance().getTime()));
        gameScore.put("time", new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()));
        gameScore.put("value", getBatteryLevel());
        gameScore.saveEventually();
    }

    public float getBatteryLevel() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level;
        try {
            level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        }catch(Exception e){
            return 0;
        }
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
                ArrayList<String> BSSID = new ArrayList<>();
                ArrayList<String> SSID = new ArrayList<>();

                for (ScanResult ap : results) {
                    BSSID.add(ap.BSSID);
                    SSID.add(ap.SSID);
                }
                try {
                    unregisterReceiver(this);
                } catch (Exception e) {
                    Log.e(TAG, "Error unregistering");
                }
                ParseObject gameScore = new ParseObject("Wifi");
                gameScore.put("MAC", Global.DEVICE_ADDRESS);
                gameScore.put("date", new SimpleDateFormat("yyyy.MM.dd").format(Calendar.getInstance().getTime()));
                gameScore.put("time", new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()));
                gameScore.addAllUnique("SSID", SSID);
                gameScore.addAllUnique("BSSID", BSSID);
                gameScore.saveEventually();
            }
        }, filter);

        // start WiFi Scan
        mWifiManager.startScan();


    }

    /*
            BLUETOOTH RELATED METHODS and Bt device management methods
     */
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

    //Called on connection success. The Quick Start App immediately tries to
    //authenticate a user (or, if a user is already authenticated, return to the conversation
    //screen).
    public void onConnectionConnected(LayerClient client) {
        Log.v(TAG, "Connected to Layer");

        //If the user is already authenticated (and this connection was being established after
        // the app was disconnected from the network), then start the conversation view.
        //Otherwise, start the authentication process, which effectively "logs in" a user
        if (client.isAuthenticated()){
            Log.i(TAG,"Client is authenticated");
        }
        else
            client.authenticate();

    }

    //Called when the connection is closed
    public void onConnectionDisconnected(LayerClient client) {
        Log.v(TAG, "Connection to Layer closed");
    }

    //Called when there is an error establishing a connection. There is no need to re-establish
    // the connection again by calling layerClient.connect() - the SDK will handle re-connection
    // automatically. However, this callback can be used with conjunction with onConnectionConnected
    // to provide feedback to the user that messages cannot be sent/received (assuming there is an
    // authenticated user).
    public void onConnectionError(LayerClient client, LayerException e) {
        Log.v(TAG, "Error connecting to layer: " + e.toString());
    }

    //Called after layerClient.authenticate() executes
    //You will need to set up an Authentication Service to take a Layer App ID, User ID, and the
    //nonce to create a Identity Token to pass back to Layer
    //NOTES:
    // - The method will be called when you call "layerClient.authenticate()" or after
    // Authentication
    //   when the Identity Token generated by your Web Services expires (you explicitly need to set
    //   the expiration date in the Token)
    // - The Nonce returned in this function will expire after 10 minutes, after which you will need
    //   to call
    public void onAuthenticationChallenge(final LayerClient client, final String nonce) {
        Log.d(TAG, "Authenticating with nonce: " + nonce);

        // Make a request to your backend to acquire a Layer identityToken
        HashMap<String, Object> params = new HashMap<>();
        params.put("userID", Global.DEVICE_ADDRESS);
        params.put("nonce", nonce);
        ParseCloud.callFunctionInBackground("generateToken", params, new FunctionCallback<String>() {
            public void done(String token, ParseException e) {
                if (e == null) {
                    client.answerAuthenticationChallenge(token);
                } else {
                    Log.d(TAG, "Parse Cloud function failed to be called to generate token with error: " + e.getMessage());
                }
            }
        });
    }

    //Called when the user has successfully authenticated
    public void onAuthenticated(LayerClient client, String userID) {

        //Start the conversation view after a successful authentication
        Log.v(TAG, "Authentication successful");
    }

    //Called when there was a problem authenticating
    //Common causes include a malformed identity token, missing parameters in the identity token,
    // missing
    //or incorrect nonce
    public void onAuthenticationError(LayerClient layerClient, LayerException e) {
        Log.v(TAG, "There was an error authenticating: " + e);
    }

    //Called after the user has been deauthenticated
    public void onDeauthenticated(LayerClient client) {

        Log.v(TAG, "User is deauthenticated.");
    }

    @Override
    public void onDestroy() {
        Log.i(TAG,"Exiting");
        layerClient.deauthenticate();
        layerClient.disconnect();
        logTimer.cancel();
        myTimer.cancel();
        mAdapter.disable();
        try {
            unregisterReceiver(bReceiver);

        } catch (Exception e) {
            Log.e(TAG, "UnregisterReceiver Error");
        }
    }

}

