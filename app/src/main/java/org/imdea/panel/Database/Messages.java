package org.imdea.panel.Database;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

@SuppressWarnings({"unchecked"})


public class Messages {

    /*
      Returns the messagess that hasnt beeen delivered to that device.
 */
    public static ArrayList<BtMessage> getMessages(String mac_addr, ArrayList<BtMessage> messages) {

        ArrayList<BtMessage> messages_selected = new ArrayList<>();

        for (BtMessage msg : messages) {
            if (!msg.isAlreadySent(mac_addr)) {
                    messages_selected.add(msg);
            }
        }

        return messages_selected;
    }

    public static JSONArray createTagList(ArrayList<String> tags) {
        JSONArray my_array = new JSONArray();
        for (String tag : tags) my_array.put(tag);
        return my_array;
    }

    public static String createMessage(JSONArray messages, JSONArray hashes) {

        //if (messages == null) if (hashes == null) return null;
        if (messages == null) return null;

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



}
