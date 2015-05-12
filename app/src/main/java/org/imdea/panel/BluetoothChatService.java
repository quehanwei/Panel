/*
 * Copyright (C) 2015 IMDEA Networks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.imdea.panel;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothChatService extends Service{


    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    private static final String TAG = "BluetoothChatService";       // Debugging
    private static final String NAME = "BluetoothChat"; // Name for the SDP record when creating server socket
    private static final UUID MY_UUID =UUID.fromString("61a769d5-1874-45d0-8545-2285d8f9c878");     // Unique UUID for this application

    public static SQLiteDatabase db;
    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    ArrayList<String> devices;  //List of detected devices
    ArrayList<BtMessage> messages;
    ArrayList<String> receivedMessages;
    Context context;
    private ServerThread mServerThread;
    private ClientThread mClientThread;
    private PostManThread mPostManThread;

    final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {              // When discovery finds a device
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);                   // Get the BluetoothDevice object from the Intent
                if (isNew(devices, device.getAddress())) {
                    Log.i(TAG, "Found " + device.getAddress());
                    devices.add(device.getAddress());   // add the name and the MAC address of the object to the arrayAdapter
                }
            }
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.i(TAG, "Discovery Finished");
                mAdapter.cancelDiscovery();

                mPostManThread = new PostManThread(devices);
                mPostManThread.start();

            }
            /*
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                Log.i(TAG, "Wifi Scan Finished");
                wifi.setWifiEnabled(false);
                writeLog(wifi.getScanResults().toString());

            }*/
        }
    };
    private ConnectionManager mConnectionManagerThread;
    private int mState;
    private WifiManager wifi;
    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */

    public BluetoothChatService(Context context, Handler handler) {

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;

        devices = new ArrayList<String>();
        messages = new ArrayList<BtMessage>();

        this.context = context;
        enableBt();

        //wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        context.registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND)); // Don't forget to unregister during onDestroy
        context.registerReceiver(bReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        //context.registerReceiver(bReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        final Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            public void run() {
                if (messages != null && getState() == STATE_LISTEN) {
                    startDiscovery();
                }
            }

        }, 30000, 60000);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public void enableBt() {

        if (!mAdapter.isEnabled())
            context.startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
        //if (!mAdapter.isEnabled())  mAdapter.enable();                                  //Turn On Bluetooth without Permission


        //context.startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0));
        if (mAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {

            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            context.startActivity(discoverableIntent);
        }
    }
    /**
     * Check if the new address is already stored on the arraylist
     */
    public boolean isNew(ArrayList<String> items,String newAddress){
        if(items==null) return true;
        for( int i = 0 ; i < items.size() ; i++ ){
            if(items.get(i).equals(newAddress)) return false;
        }
        return true;
    }

    void Msgnotify(BtMessage item){
        NotificationManager mNotificationManager;
        Log.i("Start", "notification");
      /* Invoking the default notification service */
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);

        mBuilder.setContentTitle(item.user);
        mBuilder.setContentText(item.msg);
        mBuilder.setTicker("New Message Alert!");

      /* Creates an explicit intent for an Activity in your app */
        Intent resultIntent = new Intent(this, NotificationView.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this).addParentStack(NotificationView.class);

      /* Adds the Intent that starts the Activity to the top of the stack */
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(resultPendingIntent);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      /* notificationID allows you to update the notification later on. */
        mNotificationManager.notify(1, mBuilder.build());
    }

    public boolean isEmpty() {
        if (messages != null) if (!messages.isEmpty()) return false;
        if (receivedMessages != null) if (!receivedMessages.isEmpty()) return false;
        return true;
    }

    public void startDiscovery() {
        if (mAdapter.isDiscovering()) mAdapter.cancelDiscovery();
        if(devices != null) devices.clear();
        if (isEmpty()) return;
        mAdapter.startDiscovery();
        Log.i(TAG, "Starting Discovery");
        final Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            public void run() {
                if (mAdapter.isDiscovering()) {
                    Log.i(TAG, "Discovering Time Exceeded");
                    mAdapter.cancelDiscovery();
                    mAdapter.enable();
                    startDiscovery();
                    myTimer.cancel();
                }
            }

        }, 20000);

    }


    public void addMessage(BtMessage Item) {
        messages.add(Item);
    }

    /*
    public void connectToDevices(){
        if(devices != null){
            for (String device : devices) {
                connect(mAdapter.getRemoteDevice(device));
                Log.i(TAG, "Waiting until STATE_CONNECTED");
                final Timer myTimer = new Timer();
                myTimer.schedule(new TimerTask() {
                    public void run() {     //Check periodically if a connection has been stablished.
                        if (getState() == STATE_CONNECTED) {
                            sendMessages();
                            myTimer.cancel();
                            //myTimer.purge();
                        }
                    }

                }, 0, 1000);


            }
        }
        //stop();
        //BluetoothChatService.this.start();

    }*/




    /*public void startLog(){
        Log.i(TAG,"Wifi Scan Initiated");
        if (wifi.isWifiEnabled() == false) wifi.setWifiEnabled(true);
        wifi.startScan();
    }

    public void writeLog(String results){
        try {
            FileWriter fstream = new FileWriter(Environment.getExternalStorageDirectory().getPath() + "/btlog.txt",true);
            PrintWriter out = new PrintWriter(fstream);
            out.print("\n\n");
            out.print(new SimpleDateFormat("mm.HH.dd.MM.yyyy").format(new Date()) +"\t"+ devices.toString() +"\t"+results);
            out.close();

        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }*/


    public void sendMessages(){
        for (int i = 0; i < messages.size(); i++) {

            Log.i(TAG, "Sending " + messages.get(i).toJson());
            write(messages.get(i).toJson().getBytes());

            //Log.i(TAG, "Sending "+ messages.get(i).msg.getBytes());
            //write(messages.get(i).msg.getBytes());
        }
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE).sendToTarget();
    }

    /**
     * Start the chat service. Specifically start ServerThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        if (!mAdapter.isEnabled())  mAdapter.enable();
        enableBt();

        // Cancel any thread attempting to make a connection
        if (mClientThread != null) {
            mClientThread.cancel();
            mClientThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectionManagerThread != null) {
            mConnectionManagerThread.cancel();
            mConnectionManagerThread = null;
        }

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if (mServerThread == null) {
            mServerThread = new ServerThread();
            mServerThread.start();
        }
    }

    /**
     * Start the ClientThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "Connecting to " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mClientThread != null) {
                mClientThread.cancel();
                mClientThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectionManagerThread != null) {
            mConnectionManagerThread.cancel();
            mConnectionManagerThread = null;
        }

        // Start the thread to connect with the given device
        mClientThread = new ClientThread(device);
        mClientThread.start();
        setState(STATE_CONNECTING);
    }

    public boolean addToDb(BtMessage item) {
        Boolean Success = false;
        DBHelper msg_database = new DBHelper(context, "messages.db", null, 1);
        db = msg_database.getWritableDatabase();
        if (!item.isGeneral) {
            if (DBHelper.TagExists(db, item.tag)) if (DBHelper.isNew(db, item)) {
                DBHelper.insertMessage(db, item);
            }
            Success = true;
        } else {
            if (DBHelper.isNew(db, item)) DBHelper.insertMessage(db, item);
            Success = true;
        }
        db.close();
        return Success;
    }
    /**
     * Start the ConnectionManager to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d(TAG, "connected, Socket");

        // Cancel the thread that completed the connection
        if (mClientThread != null) {
            mClientThread.cancel();
            mClientThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectionManagerThread != null) {
            mConnectionManagerThread.cancel();
            mConnectionManagerThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mServerThread != null) {
            mServerThread.cancel();
            mServerThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        String address = device.getAddress();
        mConnectionManagerThread = new ConnectionManager(socket);
        mConnectionManagerThread.start();
        Log.i(TAG, "Connected to " + address);
        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mClientThread != null) {
            mClientThread.cancel();
            mClientThread = null;
        }

        if (mConnectionManagerThread != null) {
            mConnectionManagerThread.cancel();
            mConnectionManagerThread = null;
        }

        if (mServerThread != null) {
            mServerThread.cancel();
            mServerThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectionManager in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectionManager#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectionManager r;
        // Synchronize a copy of the ConnectionManager
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectionManagerThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        Log.e(TAG, "Device connection was lost");
        BluetoothChatService.this.start();          // Start the service over to restart listening mode
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class ServerThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public ServerThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "Socket BEGIN mServerThread: " + this);

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a successful connection or an exception
                    socket = mmServerSocket.accept();
                    /*
                     It will return when either a connection has been accepted or an exception has occurred.
                     A connection is accepted only when a remote device has sent a connection request with
                     a UUID matching the one registered with this listening server socket.
                     When successful, accept() will return a connected BluetoothSocket.
                     */
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothChatService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                Log.i(TAG,"Starting Connected Thread");
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mServerThread");

        }

        public void cancel() {
            Log.d(TAG, "Socket cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type close() of server failed", e);
            }
        }
    }



            /*
    public void connectToDevices(){
        if(devices != null){
            for (String device : devices) {
                connect(mAdapter.getRemoteDevice(device));
                Log.i(TAG, "Waiting until STATE_CONNECTED");
                final Timer myTimer = new Timer();
                myTimer.schedule(new TimerTask() {
                    public void run() {     //Check periodically if a connection has been stablished.
                        if (getState() == STATE_CONNECTED) {
                            sendMessages();
                            myTimer.cancel();
                            //myTimer.purge();
                        }
                    }

                }, 0, 1000);


            }
        }
        //stop();
        //BluetoothChatService.this.start();

    }*/

    private class PostManThread extends Thread {

        ArrayList<String> devices;

        public PostManThread(ArrayList<String> devices) {
            this.devices = devices;
        }

        public void run() {
            Log.i(TAG, "BEGIN mPostManThread");
            if (messages != null) sendMessages();
            if (receivedMessages != null) sendHashes();
            Log.i(TAG, "END mPostManThread");
            BluetoothChatService.this.start();
            return;
        }

        public void sendMessages() {
            Log.i(TAG, "STEP 1: MESSAGES");
            for (String device : devices) {
                for (BtMessage message : messages) {
                    Log.i(TAG, "Waiting until STATE_NONE or STATE_LISTEN");
                    while (BluetoothChatService.this.getState() != STATE_LISTEN) {
                    }
                    connect(mAdapter.getRemoteDevice(device));
                    Log.i(TAG, "Waiting until STATE_CONNECTED");
                    while (BluetoothChatService.this.getState() != STATE_CONNECTED) {
                        try {
                            Thread.sleep(100);
                        } catch (Exception e) {

                    }
                    }
                    Log.i(TAG, "Sending " + message.toJson());
                    write(message.toJson().getBytes());
                    BluetoothChatService.this.stop();

                }
            }
        }

        public void sendHashes() {
            Log.i(TAG, "STEP 2: CHECKSUM");
            for (String device : devices) {
                for (String hash : receivedMessages) {
                    Log.i(TAG, "Waiting until STATE_NONE or STATE_LISTEN");
                    while (BluetoothChatService.this.getState() != STATE_LISTEN) {
                    }
                    connect(mAdapter.getRemoteDevice(device));
                    Log.i(TAG, "Waiting until STATE_CONNECTED");
                    while (BluetoothChatService.this.getState() != STATE_CONNECTED) {
                    }
                    Log.i(TAG, "Sending HASH" + hash);
                    write(hash.getBytes());
                    BluetoothChatService.this.stop();
                }
            }
        }

        public void sendTags(ArrayList<String> tags) {
            Log.i(TAG, "BEGIN Tags Sync");
            for (String device : devices) {
                connect(mAdapter.getRemoteDevice(device));
                Log.i(TAG, "Waiting until STATE_CONNECTED");
                while (BluetoothChatService.this.getState() != STATE_CONNECTED) {
                }
                Log.i(TAG, "Sending " + tags.toString());
                write(tags.toString().getBytes());
                BluetoothChatService.this.stop();
                while (BluetoothChatService.this.getState() != STATE_NONE || BluetoothChatService.this.getState() != STATE_LISTEN) {
                }
            }
            Log.i(TAG, "END  Tags Sync");
            BluetoothChatService.this.start();
            return;
        }


    }
    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ClientThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ClientThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try {
                Log.i(TAG,"Creating Socket for the connection");
                tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mClientThread (run method )" );
            mAdapter.cancelDiscovery();             // Always cancel discovery because it will slow down a connection
            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a succesul connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();                   // Close the socket
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                Log.e(TAG, "Unable to connect device",e);
                BluetoothChatService.this.stop();
                BluetoothChatService.this.start();  // Start the service again to restart listening mode
                return;
            }

            // Reset the ClientThread because we're done
            synchronized (BluetoothChatService.this) {
                mClientThread = null;
            }
            connected(mmSocket, mmDevice);              // Start the connected thread

        }

        public void cancel() {
            try {
                Log.i(TAG,"ClientThread Close");
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectionManager extends Thread {

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectionManager(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectionManagerThread");
            byte[] buffer = new byte[2048];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);                        // Read from the InputStream
                    String readMessage = new String(buffer, 0, bytes);  // construct a string from the valid bytes in the buffer
                    Log.i(TAG, "RECEIVED: " + readMessage);

                    JSONObject json_object;

                    try {

                        json_object = new JSONObject(readMessage);
                        BtMessage item = new BtMessage(json_object);
                        item.updateHits();
                        if (addToDb(item)) {
                            addMessage(item);
                            Msgnotify(item);
                            mHandler.obtainMessage(Constants.MESSAGE_READ).sendToTarget();                       // Send the obtained bytes to the UI Activity
                            receivedMessages.add(item.tohash());
                            Log.i(TAG, item.tohash());
                        }

                    }
                    catch(Exception e){
                        Log.e(TAG, "Impossible to parse JSON as a message");
                    }
                    try {
                        String hash = new JSONObject(readMessage).getString("received");
                        for (BtMessage message : messages) {
                            if (message.tohash().equals(hash)) {
                                Log.i(TAG, "Message received by peer: " + message.toString());
                                messages.remove(message);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Impossible to parse JSON as a checksum");
                    }
                    json_object = null;

                    // Send the obtained bytes to the UI Activity

                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {

            try {
                mmOutStream.write(buffer);
                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(Constants.MESSAGE_WRITE).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
            //BluetoothChatService.this.start();            POSTMAN LO HA QUITADO
        }

        public void cancel() {
            try {
                Log.i(TAG,"ConnetionManager Socket CLose");
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
