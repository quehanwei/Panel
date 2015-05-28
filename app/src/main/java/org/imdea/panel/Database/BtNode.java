package org.imdea.panel.Database;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class BtNode {

    /*
    Esta lista guarda los Hashes de los mensajes recibidos por este nodo.
    Esta informacion en enviada a este node cuandop se hace un paquete para el in order to allow resending messages.
     */
    public ArrayList<String> rx_msg_hash;
    /*
    This list saves the hashes of the messages sent by us to that node and that their arrival has been confirmed.
     */
    public ArrayList<String> tx_msg_hash;
    public String MAC;
    String date;
    String time;
    String LM_time;
    String LM_date;
    String name;

    public BtNode(String mac) {
        rx_msg_hash = new ArrayList<>();
        tx_msg_hash = new ArrayList<>();
        this.MAC = mac;
        this.date = new SimpleDateFormat("MM.dd.yyyy").format(new Date());
        this.time = new SimpleDateFormat("HH:mm").format(new Date());
        this.LM_date = new SimpleDateFormat("MM.dd.yyyy").format(new Date());
        this.LM_time = new SimpleDateFormat("HH:mm").format(new Date());

    }

    public void setName(String name) {
        this.name = name;
    }


    /*
    method to add the hashes captured in the incoming messages, hasehes that give us information about the messages this device sent to us
            1-Refresh the Last_mod_date
            2-looks for a duplicate hash, if not, save it
     */
    public void addTx(String hash) {
        boolean isNew = true;
        this.LM_date = new SimpleDateFormat("MM.dd.yyyy").format(new Date());
        this.LM_time = new SimpleDateFormat("HH:mm").format(new Date());

        // Now we check if the hash already exists
        for (String h : tx_msg_hash) {
            if (h.equals(hash)) {
                isNew = false;
                break;
            }
        }
        // If not exists we add the hash to the database
        if (isNew) tx_msg_hash.add(hash);
    }


    public void addRx(String hash) {
        boolean isNew = true;
        this.LM_date = new SimpleDateFormat("MM.dd.yyyy").format(new Date());
        this.LM_time = new SimpleDateFormat("HH:mm").format(new Date());

        // Now we check if the hash already exists
        for (String h : rx_msg_hash) {
            if (h.equals(hash)) {
                isNew = false;
                break;
            }
        }
        // If not exists we add the hash to the database
        if (isNew) rx_msg_hash.add(hash);
    }

    public boolean HasBeenSent(String hash) {
        for (String h : rx_msg_hash) {
            if (h.equals(hash)) return true;
        }
        return false;
    }

    public boolean HasBeenReceived(String hash) {
        for (String h : tx_msg_hash) {
            if (h.equals(hash)) return true;
        }
        return false;
    }


    // RX sirve para limitar los envios
    // TX sirve para enviar los hashes

}


