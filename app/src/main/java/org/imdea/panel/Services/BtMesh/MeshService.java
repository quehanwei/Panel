package org.imdea.panel.Services.BtMesh;

import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.imdea.panel.Database.BtMessage;
import org.imdea.panel.Database.DBHelper;
import org.imdea.panel.Global;
import org.imdea.panel.MainActivity;
import org.imdea.panel.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/*
    Code Based on BluetoothMeshNetwork project (https://github.com/udiboy1209/BluetoothMeshNetwork) by  Meet Udeshi ( @udiboy1209 )
    mudeshi1209@iitb.ac.in
 */

public class MeshService extends Service {
    private List<Device> devices;
    private List<BtMessage> broadcasts;
    private int continueFrom=0;
    private Device connectedDevice;
    final String TAG = "MeshService";
    private BluetoothChatHelper bluetoothHelper;
    public SharedPreferences SP;

    private BluetoothAdapter mBluetoothAdapter = null;
    public boolean isBroadcasting = false;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatHelper.STATE_CONNECTED:
                            //setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            //msgList.removeAllViews();
                            break;
                        case BluetoothChatHelper.STATE_CONNECTING:
                            //setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatHelper.STATE_LISTEN:
                        case BluetoothChatHelper.STATE_NONE:
                            //setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    //addToConversation("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    BtMessage pckt = new BtMessage(readBuf.toString());

                    // TODO display msg
                    Toast.makeText(getApplicationContext(), pckt.toString(), Toast.LENGTH_LONG).show();
                    Log.w("Broadcast", pckt.toJson().toString());

                    // Check then broadcast
                    for (BtMessage broadcasted : broadcasts ){
                        if(broadcasted.toHash().equals(pckt.toHash())) {
                            Log.i("Broadcast", "Looped back : " + pckt.toHash());
                            break;
                        }
                    }

                    for(Device device : devices){
                        if(!device.getId().equals(connectedDevice.getId()))
                            device.sendPacket(pckt);
                    }
                    break;
                case Constants.MESSAGE_CONNECTED:
                    String addr1 = msg.getData().getString(Constants.DEVICE_ADDRESS);

                    connectedDevice = getDevice(addr1);
                    break;
                case Constants.MESSAGE_CONNECT_FAILED:
                    //String addr2 = msg.getData().getString(Constants.DEVICE_ADDRESS);

                    //getDevice(addr2).setState(Device.STATE_BUSY);
                    break;
            }
        }
    };
    private final TimerTask discoverProcess = new TimerTask(){
        public void run(){
            broadcasts = DBHelper.recoverLiveMessages(Global.db, Global.max_send_n);

            for(BtMessage msg : broadcasts){
                broadcastMessage(msg);
            }

            if(!isBroadcasting)
            doDiscovery();
        }
    };

    private final TimerTask queuePoller = new TimerTask() {
        @Override
        public void run() {
            if(mBluetoothAdapter.isDiscovering()==false){
                isBroadcasting = true;
                Log.i("Poller", "Poller running");
                if (bluetoothHelper.getState() == BluetoothChatHelper.STATE_LISTEN || bluetoothHelper.getState() == BluetoothChatHelper.STATE_NONE) {
                    int i;
                    for (i = continueFrom; i < devices.size(); i++) {
                        Log.i(TAG,"DeviceState "+ devices.get(i).getState() +" Queue: " + devices.get(i).queueSize());
                        if (devices.get(i).getState() == Device.STATE_IDLE && devices.get(i).queueSize() > 0) {
                            Log.i("Poller", "Connecting to : " + devices.get(i).btDevice.getName());
                            bluetoothHelper.connect(devices.get(i), false);
                            break;
                        }
                    }

                    Log.i("Poller", "i: " + i);
                    Log.i("Poller", "Devices size: " + devices.size());

                    if (devices.size() > 0)
                        continueFrom = (i < devices.size() ? i + 1 : i) % devices.size();
                    Log.i(TAG,"ContinueFrom" +  continueFrom);
                }
                    isBroadcasting = false;
        }
        }
    };

    private Timer timer = new Timer("QueuePollThread");
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        }

    public MeshService() {
        devices = new ArrayList<>();
        broadcasts = new ArrayList<>();
        bluetoothHelper = new BluetoothChatHelper(this, mHandler);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SP = PreferenceManager.getDefaultSharedPreferences(this);

        Global.refresh_freq = Integer.parseInt(SP.getString("sync_frequency", "60"));
        Global.max_send_n = Integer.parseInt(SP.getString("ttl_opt", "300"));

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

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

        timer.scheduleAtFixedRate(queuePoller, 1000, 2500);
        timer.scheduleAtFixedRate(discoverProcess, 3000, 1000*30);


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        bluetoothHelper.start();

        return START_STICKY;
    }

    public void doDiscovery() {

        Log.i(TAG, "doDiscovery()");
        // If we're already discovering, stop it
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBluetoothAdapter.startDiscovery();
    }


    public void pairWith(String deviceAddress){
        Device device = new Device(mBluetoothAdapter.getRemoteDevice(deviceAddress));

        bluetoothHelper.connect(device, true);
    }

    public void broadcastMessage(BtMessage p){
        for(Device d : devices){
            d.sendPacket(p);
        }
    }

    public Device addDevice(BluetoothDevice device){
        for(Device d : devices) {
            if(d.btDevice.getAddress().equals(device.getAddress())){
                return d;
            }
        }
        Device d = new Device(device);
        devices.add(d);
        return d;
    }

    /**
     * The BroadcastReceiver that listens for discovered devices and changes the title when
     * discovery is finished
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                Log.w(TAG,"New Device: "+  device.getAddress());
                if(isNewDevice(device.getAddress())) {
                    if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        devices.add(new Device(device));
                    }
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.i("MeshService", "Finished discovery");

            }
        }
    };

    public boolean isNewDevice(String addr){
        for(Device dev:devices){
            if(dev.btDevice.getAddress().equals(addr)) return false;
        }
        return true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public Device getDevice(BluetoothDevice device) {
        for(Device d : devices){
            if(d.btDevice.getAddress().equals(device.getAddress())){
                return d;
            }
        }

        return addDevice(device);
    }

    public Device getDevice(String address) {
        for (Device d : devices) {
            if (d.btDevice.getAddress().equals(address)) {
                return d;
            }
        }

        return addDevice(mBluetoothAdapter.getRemoteDevice(address));
    }
}
