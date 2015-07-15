/*
 * Copyright (C) 2015 IMDEA Networks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.imdea.panel.Database;

import android.util.Log;

import org.imdea.panel.Global;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class BtMessage {

    public boolean isImage;
    public int TTL;
    public String msg;
    public String user;
    public String tag;

    public String origin_time;
    public String origin_date;


    public String last_date;
    public String last_time;


    public String origin_mac_address;   // Device that created the content
    public String last_mac_address;     // device that forwardede the content
    public ArrayList<String> devices; // This is a list of MAC address of the devices that return to me the hash isgnature of that message

    public boolean isMine;  // Flag that told me if I generate the content or not without cheking the MAC.

    public int hits;
    public boolean isGeneral;

    public String wifiSSID;
    public String wifiBSSID;

    // That is the basic constructor of the Output Message
    public BtMessage(String msg,String user){
        this.msg = msg;
        this.user = user;
        this.isGeneral = true;
        this.tag = "";
        this.origin_mac_address = Global.DEVICE_ADDRESS;
        this.last_mac_address = origin_mac_address;
        this.origin_date = new SimpleDateFormat("MM.dd.yyyy").format(new Date());
        this.origin_time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        this.last_date = origin_date;
        this.last_time = origin_time;
        this.devices = new ArrayList<>();
        this.hits = 0;
        this.isImage = false;
        this.TTL = 0;
        this.isMine = false;


    }

    // This Constructor is to recover messages from the SQL Database
    public BtMessage(String origin_mac, String last_mac, String user, String message, String origin_date, String origin_time, String last_date, String last_time, String devices, int hits, int TTL, int isImage) {
        this.msg = message;
        this.user = user;
        this.isGeneral = true;
        this.tag = "";
        this.origin_mac_address = origin_mac;
        this.last_mac_address = last_mac;
        this.origin_date = origin_date;
        this.origin_time = origin_time;
        this.last_date = last_date;
        this.last_time = last_time;
        this.devices = new ArrayList<>();
        this.hits = hits;
        this.isImage = isImage > 0;
        this.TTL = TTL;
        this.isMine = false;


        String[] macs = devices.replace("[", "").replace("]", "").split(", ");

        for (String mac : macs) this.devices.add(mac);
    }

    /* This Method create a message from the JSONObject received

  */
    public BtMessage(String rawmsg) {
        JSONObject json_item;
        try {
            json_item = new JSONObject(rawmsg);
        } catch (Exception e) {
            return;
        }
        try {
            this.user = json_item.getString("user");
        } catch (Exception e) {
            Log.e("BtMesssage", "Parsing Error");
        }
        try {
            this.msg = json_item.getString("msg");
        } catch (Exception e) {
            Log.e("BtMesssage", "Parsing Error");

        }
        try {
            this.tag = json_item.getString("tag");
        } catch (Exception e) {
            Log.e("BtMesssage", "Parsing Error");

        }
        try {
            this.isGeneral = json_item.getBoolean("isGeneral");
        } catch (Exception e) {
            Log.e("BtMesssage", "Parsing Error");

        }
        try {
            this.origin_mac_address = json_item.getString("mac");
        } catch (Exception e) {
            Log.e("BtMesssage", "Parsing Error");

        }
        try {
            this.origin_date = json_item.getString("date");
        } catch (Exception e) {
            Log.e("BtMesssage", "Parsing Error");

        }
        try {
            this.origin_time = json_item.getString("time");
        } catch (Exception e) {
            Log.e("BtMesssage", "Parsing Error");

        }
        try {
            this.isImage = json_item.getBoolean("isImage");
        } catch (Exception e) {
            Log.e("BtMesssage", "Parsing Error");

        }
        try {
            this.TTL = json_item.getInt("TTL");
        } catch (Exception e) {
            Log.e("BtMesssage", "Parsing Error");

        }

        try {
            this.last_mac_address = json_item.getString("lastmac");
        } catch (Exception e) {
            Log.e("BtMesssage", "Parsing Error");
        }

        this.last_date = new SimpleDateFormat("MM.dd.yyyy").format(new Date());
        this.last_time = new SimpleDateFormat("HH:mm:ss").format(new Date());

    }

    /* This Method create a message from the JSONObject received

      */
    public BtMessage(JSONObject json_item, String mac_addr) {
        try {
            this.user = json_item.getString("user");
        }catch(Exception e){
            Log.e("BtMesssage", "Parsing Error");
        }
        try {
        this.msg = json_item.getString("msg");
        } catch (Exception e) {
            Log.e("BtMesssage", "Parsing Error");

        }
        try {
        this.tag = json_item.getString("tag");
        }catch(Exception e){
            Log.e("BtMesssage", "Parsing Error");

        }
        try {
        this.isGeneral = json_item.getBoolean("isGeneral");
        }catch(Exception e){
            Log.e("BtMesssage", "Parsing Error");

        }
        try {
            this.origin_mac_address = json_item.getString("mac");
        } catch (Exception e) {
            Log.e("BtMesssage", "Parsing Error");

        }
        try {
            this.origin_date = json_item.getString("date");
        } catch (Exception e) {
            Log.e("BtMesssage", "Parsing Error");

        }
        try {
            this.origin_time = json_item.getString("time");
        } catch (Exception e) {
            Log.e("BtMesssage", "Parsing Error");

        }
        try {
            this.isImage = json_item.getBoolean("isImage");
        } catch (Exception e) {
            Log.e("BtMesssage", "Parsing Error");

        }
        try {
            this.TTL = json_item.getInt("TTL");
        } catch (Exception e) {
            Log.e("BtMesssage", "Parsing Error");

        }
        this.last_mac_address = mac_addr;
        this.last_date = new SimpleDateFormat("MM.dd.yyyy").format(new Date());
        this.last_time = new SimpleDateFormat("HH:mm:ss").format(new Date());

    }

    public void setContext(String name, String mac) {
        this.wifiSSID = name;
        this.wifiBSSID = mac;
    }

    public boolean isInContext(String name) {
        if (this.wifiSSID.equals(name)) return true;
        else return false;
    }

    public boolean isOutdated() {
        if (this.origin_date.equals(new SimpleDateFormat("MM.dd.yyyy").format(new Date())))
            return false;
        else return true;
    }

    // Methods to provide hastag characteristics
    public void setTag(String tag){
        this.tag = tag;
        this.isGeneral = false;
    }

    // Methods to provide some network characteristics

    public void updateHits(){
        this.hits++;
    }

    public void addDevice(String addr) {
        if (!isAlreadySent(addr)) devices.add(addr);
    }

    /* Tell us if the message has been already sent to the given mac address

     */
    public boolean isAlreadySent(String addr) {

        if (devices == null) return false;

        if (devices.isEmpty()) return false;

        if (addr.equals(last_mac_address)) return true;
        if (addr.equals(origin_mac_address)) return true;

        for (String dev : devices) {
            if (dev.equals(addr)) return true;
        }

        return false;

    }

    public String toString(){
        return "'" + origin_mac_address + "', '" + user + "', '" + msg + "', '" + tag + "'";
        //return orgin + mac + message


    }

    /* Prepares the Json Object that is going to be sent

     */
    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        try {
            object.put("user", this.user);
            object.put("msg", this.msg);
            object.put("mac", this.origin_mac_address);
            object.put("isGeneral", this.isGeneral);
            object.put("tag", this.tag);
            object.put("date", this.origin_date);
            object.put("time", this.origin_time);
            object.put("TTL", this.TTL);
            object.put("isImage", this.isImage);
            object.put("lastmac", this.last_mac_address);
            object.put("context", this.wifiSSID);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object;
    }

    public String toHash() {
        String s = "";
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(this.toString().getBytes(), 0, this.toString().length());
            s = new BigInteger(1, m.digest()).toString(16);
        } catch (Exception e) {
            Log.e("BtMesssage", "Error generating hash");
        }
        return s;
    }


    public String toAck() {
        JSONObject object = new JSONObject();
        try {
            object.put("ACK", toHash());

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object.toString();
    }

    public String devicesToString() {
        String s = "";
        if (devices != null) {
            for (String dev : devices) {
                s = s + dev + "\n";
            }
        }
        return s;
    }
}
