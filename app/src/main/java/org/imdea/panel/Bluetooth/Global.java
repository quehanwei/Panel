package org.imdea.panel.Bluetooth;

import org.imdea.panel.Database.BtMessage;
import org.imdea.panel.Database.BtNode;

import java.util.ArrayList;

public class Global {

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 4;
    public static final int MESSAGE_READ = 5;
    public static final int MESSAGE_WRITE = 6;
    public static final int MESSAGE_DEVICE_NAME = 7;
    public static final int MESSAGE_TOAST = 8;
    public static final int CONECTION_FAILED = 9;

    public static volatile ArrayList<BtMessage> messages = new ArrayList<BtMessage>();
    public static volatile ArrayList<BtNode> nodes = new ArrayList<BtNode>();

    public static String DEVICE_NAME = "";

    public static String DEVICE_ADDRESS = "";
}
