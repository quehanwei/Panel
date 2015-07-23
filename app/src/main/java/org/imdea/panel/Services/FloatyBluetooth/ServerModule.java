package org.imdea.panel.Services.FloatyBluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.ObjectInputStream;
import java.lang.reflect.Method;

public class ServerModule extends Thread {

    private static ObjectInputStream ois;
    final String TAG = "Server";
    private BluetoothServerSocket tmp;
    private BluetoothSocket socket;
    private int channel;

    public ServerModule(int i) {
        this.channel = i;

    }

    public void run() {

        Log.i(TAG, "Accept Thread Started");
        boolean flag = true;

        Method m;
        try {
            m = BluetoothAdapter.class.getMethod("listenUsingInsecureRfcommOn", new Class[]{int.class});
            tmp = (BluetoothServerSocket) m.invoke(BluetoothAdapter.getDefaultAdapter(), channel);
        } catch (Exception e) {
            Log.e(TAG, "ERR Reflecting", e);
        }


        while (flag) {

            try {

                socket = tmp.accept();                  // Keep listening until exception occurs or a socket is returned

                Log.i(TAG, "New connection on channel : " + channel);
                //tmp.close();

                // Read the stream to identify to purpose of the query
                ois = new ObjectInputStream(socket.getInputStream());
                Object obj = ois.readObject();
                final String s = obj.toString();

                // First encounter sub-section


            } catch (NullPointerException e) {
                e.printStackTrace();
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {

                try {
                    synchronized (this) {
                        Log.d(TAG, "Closing Socket");
                        ois.close();
                        socket.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

