package org.imdea.panel.Bluetooth;

import android.database.sqlite.SQLiteDatabase;

public class Global {

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 4;
    public static final int MESSAGE_READ = 5;
    public static final int MESSAGE_WRITE = 6;
    public static final int MESSAGE_DEVICE_NAME = 7;
    public static final int MESSAGE_TOAST = 8;
    public static final int CONECTION_FAILED = 9;

    public static SQLiteDatabase db;

    public static String DEVICE_NAME = "";

    public static String DEVICE_ADDRESS = "00:00:00:00:00";

}


