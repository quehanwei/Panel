package org.imdea.panel.Services.FloatyBluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;

public class ClientModule extends AsyncTask<String, Integer, Boolean> {

    private static ObjectOutputStream outs;
    private static ObjectInputStream oins;
    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;
    private TextView recmsgs;
    private String purpose;
    private WakeLock wl;

    private String TAG = "ClientModule";

    private int channelNumber = 1;

    public ClientModule(BluetoothDevice device, Handler mHandler, TextView tv, String s) {

        BluetoothSocket tmp = null;         // Use a temporary object that is later assigned to mmSocket, because mmSocket is final

        try {

            Method m = device.getClass().getMethod("createInsecureRfcommSocket", new Class[]{int.class});
            tmp = (BluetoothSocket) m.invoke(device, channelNumber);

            System.out.println("\nCreated a static application address");
        } catch (Exception e) {

        }

        this.mmDevice = device;
        this.mmSocket = tmp;
        this.recmsgs = tv;
        this.purpose = s;

    }

    protected void onPreExecute() {
        System.out.println("AsyncTask Called..");
    }

    protected Boolean doInBackground(String... params) {

        try {


            /*
            PowerManager pm = (PowerManager) tgetSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "YOUR TAG");
            wl.acquire();
             */


            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

            // Connect the device through the socket. This will block until it succeeds or throws an exception
            System.out.println("\nTrying to connect to: " + mmDevice.getName() + " - " + mmDevice.getAddress());
            long start = System.currentTimeMillis();
            mmSocket.connect();
            long end = System.currentTimeMillis();

            System.out.println("\nClient is now connected");
            Log.i(TAG, "AsyncTask - Paired Node: " + mmDevice.getName());
            Log.i(TAG, Long.toString(end - start));

            if (purpose.equals("Connecting")) {

                // Send to the receiver the connection purpose
                outs = new ObjectOutputStream(mmSocket.getOutputStream());
                outs.writeObject("Connecting|");
                outs.flush();

                // Wait for him to send me his data
                oins = new ObjectInputStream(mmSocket.getInputStream());
                Object obj1 = oins.readObject();

                // Updating the local log and devices table

                // Sending also my current data back
                //outs.writeObject(DDB.returnDataTable().getList());
                outs.flush();

                // Add the device to the ones that have my content (latest)
                //DDB.addContentdevice(mmDevice);

            } else {

                // Data Transfer sub-procedure
                if (BluetoothAdapter.getDefaultAdapter().isDiscovering())
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

                // Sending my data to the server
                Log.i(TAG, "Sending data to: " + mmDevice.getName() + " [" + mmDevice.getAddress() + "]");
                outs = new ObjectOutputStream(mmSocket.getOutputStream());
                //outs.writeObject(DDB.returnDataTable().getList());
                outs.flush();

                // Wait for him to send me his data
                oins = new ObjectInputStream(mmSocket.getInputStream());
                Object obj1 = oins.readObject();

                // Updating the local log and devices table

            }

        } catch (IllegalArgumentException e) {
            e.printStackTrace();

        } catch (IOException e) {
            // To avoid loops
            /*if(!DDB.getUnreachableDevs().contains(mmDevice) && !purpose.equals("Connecting")){
                Log.i(TAG,"Device " + mmDevice.getName() + " eligible for second chance..");
                DDB.getUnreachableDevs().add(mmDevice);
            }*/
        } catch (NullPointerException e) {
            System.err.println("*** Out of sockets.. ***");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } finally {
            cancel();
            if (wl.isHeld()) wl.release();
        }
        return null;

    }

    /**
     * Will cancel an in-progress connection, and close the socket
     */
    public synchronized void cancel() {

        if (mmSocket != null) {
            Log.d(TAG, "Closing Socket");
            try {
                outs.close();
                oins.close();
                mmSocket.close();
            } catch (Exception e) {
            }
        }

    }



}


