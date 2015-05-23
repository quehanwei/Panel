package org.imdea.panel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class BtNode {
    String date;
    String time;
    ArrayList<String> hashlist;
    String MAC;
    String name;

    public BtNode(String mac, String hash) {
        hashlist = new ArrayList<>();
        hashlist.add(hash);
        this.MAC = mac;
        this.date = new SimpleDateFormat("MM.dd.yyyy").format(new Date());
        this.time = new SimpleDateFormat("HH:mm").format(new Date());

    }

    public void setName(String name) {
        this.name = name;
    }

    public void addHash(String hash) {
        hashlist.add(hash);
    }

    public void addHash(BtMessage item) {
        hashlist.add(item.tohash());
    }

    public boolean isReceived(String s) {
        for (String hash : hashlist) {
            if (hash.equals(s)) return true;
        }
        return false;
    }

}

