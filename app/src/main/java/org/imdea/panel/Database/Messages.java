package org.imdea.panel.Database;

import org.imdea.panel.Bluetooth.Global;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class Messages {

    /*
            Creates a JSONArray with the specific messages that has to be sent to each device, excluding:
                Messages sent by the own device
                Messages I know thta device has
         */
    public static JSONArray createMessageList(String device, ArrayList<BtMessage> messages, ArrayList<BtNode> nodes) {

        JSONArray my_array = new JSONArray();

        ArrayList<BtMessage> selected_msg = new ArrayList<>();

        // First we try to find that device in the node list
        BtNode node = getNode(device, nodes);

        // If the node is in the database, pick just the nmessages that arent sent.
        if (node != null) {
            selected_msg = getMessages(node, messages);
            if (selected_msg == null) return null;
            if (selected_msg.isEmpty()) return null;
            for (BtMessage message : selected_msg) {
                my_array.put(message.toJson());
            }
        } else {
            for (BtMessage message : messages) {
                my_array.put(message.toJson());
            }
        }

        return my_array;
    }

    /*
      Returns the messagess that hasnt beeen delivered to that device.
 */
    public static ArrayList<BtMessage> getMessages(BtNode node, ArrayList<BtMessage> messages) {

        ArrayList<BtMessage> messages_selected = new ArrayList<>();

        for (BtMessage msg : messages) {
            if (!node.MAC.equals(msg.origin_mac_address)) {
                if (!node.HasBeenSent(msg.toHash())) {
                    messages_selected.add(msg);
                }
            }
        }

        return messages_selected;
    }

    public static JSONArray createTagList(ArrayList<String> tags) {
        JSONArray my_array = new JSONArray();
        for (String tag : tags) my_array.put(tag);
        return my_array;
    }

    public static JSONArray createHashList(String device, ArrayList<BtNode> nodes) {

        JSONArray my_array = new JSONArray();

        // First we try to find that device in the node list
        BtNode node = getNode(device, nodes);

        // If the node is in the database pick up that hashes
        if (node != null) {
            for (String hash : node.rx_msg_hash) my_array.put(hash);
            return my_array;

        } else {
            return null;
        }

    }

    public static String createMessage(JSONArray messages, JSONArray hashes) {

        if (messages == null) if (hashes == null) return null;

        JSONObject item = new JSONObject();
        try {
            item.put("MESSAGES", messages);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            item.put("HASH", hashes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return item.toString();
    }

    public static String createQuickAnswer(String addr, ArrayList<BtMessage> messages) {

        ArrayList<BtMessage> messages_selected = new ArrayList<>();
        JSONArray my_array = new JSONArray();
        JSONObject item = new JSONObject();

        for (BtMessage msg : messages) {
            if (!addr.equals(msg.origin_mac_address)) {
                messages_selected.add(msg);
            }
        }

        for (BtMessage message : messages_selected) {
            my_array.put(message.toJson());
        }

        try {
            item.put("MESSAGES", messages);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return item.toString();

    }

    public static BtMessage getMessage(String hash) {
        for (BtMessage msg : Global.messages) {
            if (msg.toHash().equals(hash)) return msg;
        }
        return null;
    }

    public static BtNode getNode(String device, ArrayList<BtNode> nodes) {
        for (BtNode node : nodes) {
            if (node.MAC.equals(device)) {
                return node;
            }
        }
        return null;
    }

}
