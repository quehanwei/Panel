package org.imdea.panel;

import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

public class Global {

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 4;
    public static final int MESSAGE_READ = 5;
    public static final int MESSAGE_WRITE = 6;
    public static final int MESSAGE_DEVICE_NAME = 7;
    public static final int MESSAGE_TOAST = 8;
    public static final int CONECTION_FAILED = 9;

    public static int refresh_freq = 0;
    public static int max_send_n = 0;

    public static SQLiteDatabase db;

    public static String DEVICE_NAME = "";

    public static String DEVICE_ADDRESS = "00:00:00:00:00";
    //public static String SERVER_IP = "192.168.43.55";
    public static String SERVER_IP = "172.16.6.22";
    public static int SERVER_PORT = 6464;

    public static ArrayList<String> devices = new ArrayList<>();
    public static ArrayList<String> old_devices = new ArrayList<>();

}


