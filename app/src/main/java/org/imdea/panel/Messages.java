package org.imdea.panel;

import org.imdea.panel.Database.BtMessage;
import org.imdea.panel.Database.BtNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class Messages {

    public static JSONArray createMessageList(String device, ArrayList<BtMessage> messages, ArrayList<BtNode> nodes) {
        JSONArray my_array = new JSONArray();

        // First we try to find that device in the node list
        BtNode node = getNode(device, nodes);

        // If the node is in the database, pick just the nmessages that arent sent.
        if (node != null) {
            ArrayList<BtMessage> messagges = getMessages(node, messages);
        }

        for (BtMessage message : messages) {
            my_array.put(message.toJson());
        }

        return my_array;
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
        }

        return my_array;
    }

    public static String createMessage(JSONArray messages, JSONArray hashes) {
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


    /*
          Returns the messagess that hasnt beeen delivered to that device.
     */
    public static ArrayList<BtMessage> getMessages(BtNode node, ArrayList<BtMessage> messages) {

        ArrayList<BtMessage> messages_selected = new ArrayList<>();

        for (BtMessage msg : messages) {
            if (!node.HasBeenSent(msg.toHash())) {
                messages_selected.add(msg);
            }
        }
        return messages_selected;
    }

    /**
     * @param device
     * @param nodes
     * @return
     */
    public static BtNode getNode(String device, ArrayList<BtNode> nodes) {
        for (BtNode node : nodes) {
            if (node.MAC.equals(device)) {
                return node;
            }
        }
        return null;
    }

}
