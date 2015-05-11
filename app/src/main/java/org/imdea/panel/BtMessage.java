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

package org.imdea.panel;

import android.bluetooth.BluetoothAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BtMessage {

    public String msg;
    public String user;
    public String tag;
    public String time;
    public String date;
    public String mac_address;
    public int hits;
    public boolean isGeneral;

    // That is the basic constructor of the Output Message
    public BtMessage(String msg,String user){
        this.msg = msg;
        this.user = user;
        this.isGeneral = true;
        this.tag = "";
        this.mac_address = BluetoothAdapter.getDefaultAdapter().getAddress();
        this.date = new SimpleDateFormat("MM.dd.yyyy").format(new Date());
        this.time = new SimpleDateFormat("HH:mm").format(new Date());

    }

    // That is the basic constructor of the Input Message
    public BtMessage(String mac,String msg,String user,String time,String date){
        this.msg = msg;
        this.user = user;
        this.isGeneral = true;
        this.tag = "";
        this.mac_address = mac;
        this.date = date;
        this.time = time;

    }

    public BtMessage(String mac,String msg,String user){
        this.msg = msg;
        this.user = user;
        this.isGeneral = true;
        this.tag = "";
        this.mac_address = mac;
        this.date = new SimpleDateFormat("MM.dd.yyyy").format(new Date());
        this.time = new SimpleDateFormat("HH:mm").format(new Date());

    }

    public BtMessage(JSONObject json_item,String mac){
        try {
            this.user = json_item.getString("user");
        }catch(Exception e){

        }
        try {
        this.msg = json_item.getString("msg");
    }catch(Exception e){

    }
        try {
        this.tag = json_item.getString("tag");
        }catch(Exception e){

        }
        try {
        this.isGeneral = json_item.getBoolean("isGeneral");
        }catch(Exception e){

        }
        this.mac_address = mac;
        this.date = new SimpleDateFormat("MM.dd.yyyy").format(new Date());
        this.time = new SimpleDateFormat("HH:mm").format(new Date());

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

    public String toString(){
        return "'" + mac_address + "', '" + user + "', '" + msg + "', '" + tag + "', '" + date + "', '" + time +"'";
    }

    public String toJson() {
        JSONObject object = new JSONObject();
        try {
            object.put("user", this.user);
            object.put("msg", this.msg);
            object.put("isGeneral", this.isGeneral);
            object.put("tag", this.tag);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object.toString();
    }

}
