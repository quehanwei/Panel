package org.imdea.panel;

import java.util.ArrayList;

public interface Global {


    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 4;
    public static final int MESSAGE_READ = 5;
    public static final int MESSAGE_WRITE = 6;
    public static final int MESSAGE_DEVICE_NAME = 7;
    public static final int MESSAGE_TOAST = 8;
    public static final int CONECTION_FAILED = 9;

    public static boolean connectionRefused = false;

    public static ArrayList<BtMessage> messages = new ArrayList<BtMessage>();

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

}
