package org.imdea.panel.Services.BtMesh;

import android.bluetooth.BluetoothDevice;

import org.imdea.panel.Database.BtMessage;

import java.util.ArrayList;
import java.util.List;

public class Device {
    public static final int STATE_CONNECTED=1;
    public static final int STATE_IDLE=2;
    public static final int STATE_BUSY=3;
    public static final int STATE_OUT_OF_RANGE=4;

    BluetoothDevice btDevice;
    private List<BtMessage> pcktQueue;
    private int state=STATE_IDLE;

    public Device(BluetoothDevice bt){
        btDevice = bt;
        pcktQueue = new ArrayList<>();


    }

    /**
     * @return boolean true if packet added to queue, else false
     */
    public boolean sendPacket(BtMessage newp){
        for(BtMessage p : pcktQueue){
            if(p.toHash().equals(newp.toHash()))
                return false;
        }
        pcktQueue.add(newp);

        return true;
    }

    public int queueSize(){
        return pcktQueue.size();
    }

    /**
     * @return byte data of packet popped from start
     */
    public byte[] pop(){
        return pcktQueue.remove(0).toJson().toString().getBytes();
    }

    public String getId(){
        return btDevice.getAddress();
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }
}
