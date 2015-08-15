package org.imdea.panel.Services.smoothBluetooth;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
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
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import io.palaima.smoothbluetooth.Device;
import io.palaima.smoothbluetooth.SmoothBluetooth;
import io.palaima.smoothbluetooth.SmoothBluetooth.Connection;
import io.palaima.smoothbluetooth.SmoothBluetooth.ConnectionCallback;
import io.palaima.smoothbluetooth.SmoothBluetooth.ConnectionTo;

public class smoothService extends Service implements SmoothBluetooth.Listener {

    final String TAG = "SmoothBluetooth";
    SmoothBluetooth mSmoothBluetooth;
    String deviceConnected = null;
    ArrayList<BtMessage> messages = new ArrayList<>();
    SharedPreferences SP;
    NotificationManager mNotifyMgr;
    int intentos = 0;
    private List<Integer> mBuffer = new ArrayList<>();
    private List<String> mResponseBuffer = new ArrayList<>();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        SP = PreferenceManager.getDefaultSharedPreferences(this);
        Global.refresh_freq = Integer.parseInt(SP.getString("sync_frequency", "60"));
        Global.max_send_n = Integer.parseInt(SP.getString("ttl_opt", "300"));

        final Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle("Panel is running")
                .setContentText("Tap to open it")
                .setContentIntent(PendingIntent.getActivity(this, 0, notificationIntent, 0));
        startForeground(2357, mBuilder.build());

        mSmoothBluetooth = new SmoothBluetooth(getBaseContext(), ConnectionTo.ANDROID_DEVICE, Connection.INSECURE, this);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {

                messages = DBHelper.recoverLiveMessages(Global.db, Global.max_send_n);

                if (!mSmoothBluetooth.isDiscovery()) {
                    if (!mSmoothBluetooth.isConnected())
                        mSmoothBluetooth.doDiscovery();
                    else {
                        intentos++;
                    }
                    if (intentos < 1) {
                        intentos = 0;
                        mSmoothBluetooth.disconnect();
                    }
                }
            }
        }, 1000 * 60, 1000 * 60 * 5);//put here time 1000 milliseconds=1 second * 60 = 1 minute*5 = 5 minutes
        mSmoothBluetooth.doDiscovery();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onBluetoothNotSupported() {
        Toast toast = Toast.makeText(getBaseContext(), "Bluetooth not supported!", Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    public void onBluetoothNotEnabled() {
        Toast toast = Toast.makeText(getBaseContext(), "Bluetooth not enabled!", Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    public void onConnecting(Device device) {
        Intent intent = new Intent("org.imdea.panel.STATUS_CHANGED");
        intent.putExtra("STATUS", "Connecting");
        deviceConnected = device.getAddress();
        sendBroadcast(intent); // Now we warn the app that we received a new message
    }

    @Override
    public void onConnected(Device device) {
        //called when connected to particular device
        Intent intent = new Intent("org.imdea.panel.STATUS_CHANGED");
        intent.putExtra("STATUS", "Connected");
        //deviceConnected = device.getAddress();
        Log.w(TAG, "Connected to " + device.getAddress());
        mSmoothBluetooth.send("HELLO", true);
    }

    @Override
    public void onDisconnected() {
        //called when disconnected from device
        Intent intent = new Intent("org.imdea.panel.STATUS_CHANGED");
        intent.putExtra("STATUS", "Idle");
        deviceConnected = null;
    }

    @Override
    public void onConnectionFailed(Device device) {
        Intent intent = new Intent("org.imdea.panel.STATUS_CHANGED");
        intent.putExtra("STATUS", "Error");
    }

    @Override
    public void onDiscoveryStarted() {
        Intent intent = new Intent("org.imdea.panel.STATUS_CHANGED");
        intent.putExtra("STATUS", "Discovering");
    }

    @Override
    public void onDiscoveryFinished() {
        //called when discovery is finished
    }

    @Override
    public void onNoDevicesFound() {
        Intent intent = new Intent("org.imdea.panel.STATUS_CHANGED");
        intent.putExtra("STATUS", "0 devices");
    }

    @Override
    public void onDevicesFound(final List<Device> deviceList, final ConnectionCallback connectionCallback) {
        Log.w(TAG, "Devices found:");
        for (Device item : deviceList) {
            Log.i(TAG, "\tDevice: " + item.getAddress());
        }
        for (Device item : deviceList) {
            Log.w(TAG, "Connecting to " + item.getAddress());
            connectionCallback.connectTo(item);
        }
        //receives discovered devices list and connection callback you can filter devices list and connect to specific one connectionCallback.connectTo(deviceList.get(position));
    }

    @Override
    public void onDataReceived(int data) {
        //receives all bytes
        mBuffer.add(data);
        Toast toast = Toast.makeText(getBaseContext(), data, Toast.LENGTH_SHORT);
        toast.show();
        if (data == 62 && !mBuffer.isEmpty()) {
            //if (data == 0x0D && !mBuffer.isEmpty() && mBuffer.get(mBuffer.size()-2) == 0xA0) {
            StringBuilder sb = new StringBuilder();
            for (int integer : mBuffer) {
                sb.append((char) integer);
            }
            mBuffer.clear();
            Log.w(TAG, "RECEIVED: " + sb.toString());
            showNotification("RECEIVED", sb.toString());
        }

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
}
