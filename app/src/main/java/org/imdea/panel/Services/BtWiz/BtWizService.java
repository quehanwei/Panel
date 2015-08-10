package org.imdea.panel.Services.BtWiz;

import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.imdea.panel.Database.BtMessage;
import org.imdea.panel.Database.DBHelper;
import org.imdea.panel.Global;
import org.imdea.panel.MainActivity;
import org.imdea.panel.R;
import org.imdea.panel.Services.Bluetooth.BtModule;

import com.btwiz.library.BTSocket;
import com.btwiz.library.BTWiz;
import com.btwiz.library.DeviceMajorComparator;
import com.btwiz.library.DeviceNotSupportBluetooth;
import com.btwiz.library.DiscoveryStatus;
import com.btwiz.library.GetAllDevicesListener;
import com.btwiz.library.IAcceptListener;
import com.btwiz.library.IDeviceComparator;
import com.btwiz.library.IDeviceConnectionListener;
import com.btwiz.library.IDeviceLookupListener;
import com.btwiz.library.IReadListener;
import com.btwiz.library.IWriteListener;
import com.btwiz.library.MarkCompletionListener;
import com.btwiz.library.SecureMode;
import com.btwiz.library.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class BtWizService extends Service implements IAcceptListener,IDeviceConnectionListener,IDeviceLookupListener{

    SharedPreferences SP;
    BluetoothAdapter mAdapter;
    ArrayList<String> messages = new ArrayList<>();
    final Timer myTimer = new Timer();
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    final String TAG = "BtWiz";

    public BtWizService() {
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        SP = PreferenceManager.getDefaultSharedPreferences(this);

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mAdapter.enable();

        Global.DEVICE_ADDRESS = mAdapter.getAddress();

        Global.refresh_freq = Integer.parseInt(SP.getString("sync_frequency", "60"));
        Global.max_send_n = Integer.parseInt(SP.getString("ttl_opt", "300"));

        messages = DBHelper.recoverLiveMessages(Global.db, Global.max_send_n);
        try {
            BTWiz.init(getApplicationContext());
        }catch(Exception e){

        }
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

        Log.i(TAG, "Refresh Freq set to: " + Global.refresh_freq);
        Log.i(TAG, "Max Resend Number set to: " + Global.max_send_n);

        listenForConnections();

        myTimer.schedule(new TimerTask() {
            public void run() {

                messages = DBHelper.recoverLiveMessages(Global.db, Global.max_send_n);
                if (messages != null & messages.size() > 0) {
                   discoverDevices();

                } else {

                }

            }

        }, 30000, Global.refresh_freq * 1000);
        return Service.START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onDeviceFound(BluetoothDevice bluetoothDevice, boolean b) {
        // log
        String name = bluetoothDevice.getName();
        String addr = bluetoothDevice.getAddress();
        int major = bluetoothDevice.getBluetoothClass().getMajorDeviceClass();
        String majorStr = Utils.majorToString(major);
        Log.i("Tester", "Discovered device: " + name + ", " + addr + ", " + majorStr);
        // and connect to the newly found device

        Log.i(TAG,"Connecting...");
        BTWiz.connectAsClientAsync(getApplicationContext(),bluetoothDevice,this,SecureMode.INSECURE,MY_UUID_INSECURE);

        return false;
    }

    @Override
    public void onDeviceNotFound(boolean b) {
        Log.w(TAG, "No devices found!!");
    }

    @Override
    public void onNewConnectionAccepted(BTSocket btSocket) {
        // log

        BluetoothDevice device = btSocket.getRemoteDevice();
        String name = device.getName();
        String addr = device.getAddress();
        int major = device.getBluetoothClass().getMajorDeviceClass();
        String majorStr = Utils.majorToString(major);
        Log.i("Tester", "New connection: " + name + ", " + addr + ", " + majorStr);
        final byte[] input_buffer = null;
        btSocket.readAsync(input_buffer, new IReadListener() {
            @Override
            public void onSuccess(int i) {
                Log.w(TAG, "MSG: " + input_buffer.toString());
            }

            @Override
            public void onError(int i, IOException e) {
                Log.e(TAG, "Error receiving the MSG", e);

            }
        });

    }

    @Override
    public void onError(Exception e, String s) {
        Log.e(TAG, "Error accepting a connection:" + s + "->", e);
    }

    @Override
    public void onConnectSuccess(BTSocket btSocket) {
        Log.i(TAG, "Connected to remote device");
        String msg = "HOLA";
        btSocket.writeAsync(msg.getBytes(), new IWriteListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Message sent succesfully");
                BTWiz.cleanup(getApplicationContext());

            }

            @Override
            public void onError(IOException e) {
                Log.e(TAG, "Error sending Message",e);

            }
        });

    }

    @Override
    public void onConnectionError(Exception e, String s) {
        Log.e(TAG,"Error making a connection:" +s+ "->",e);


    }

    public void discoverDevices() {

        Log.i(TAG, BTWiz.getDiscoveryStatus().toString());

        if(!BTWiz.getDiscoveryStatus().equals(DiscoveryStatus.STARTED)){
            Log.i(TAG,"Starting Discovery");
            final MarkCompletionListener completeListener = new MarkCompletionListener();
            BTWiz.startDiscoveryAsync(getApplicationContext(),completeListener, this);

        }
        else{
            Log.i(TAG, "Discovery already running");
            BTWiz.cancelDiscovery(getApplicationContext());
            BTWiz.stopListening();
            BTWiz.closeAllOpenSockets();
            BTWiz.cleanup(getApplicationContext());

            try {
                BTWiz.init(getApplicationContext());
            }catch(Exception e){

            }
            listenForConnections();

        }

    }

    public void sendMessage(BtMessage item, ArrayList<BluetoothDevice> devices){
        if(devices==null) return;
        if(devices.isEmpty()) return;
        for(BluetoothDevice device:devices){

        }

    }

    public void listenForConnections() {

        Log.i(TAG, "Listening for connections");

        BTWiz.listenForConnectionsAsync("Floaty", this, SecureMode.INSECURE);
    }

}