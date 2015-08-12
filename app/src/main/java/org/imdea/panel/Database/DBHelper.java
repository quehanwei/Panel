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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.util.ArrayList;

public class DBHelper extends SQLiteOpenHelper {

    private static final String TAG = "DBHelper";
    public static String datafields = "origin_mac, last_mac, user, message, origin_date, origin_time, last_date, last_time, devices, hits, TTL, isImage, isMine";

    public DBHelper(Context contexto, String nombre, SQLiteDatabase.CursorFactory factory, int version) {

        super(contexto, nombre, factory, version);

    }

    public static void newTag(SQLiteDatabase db, String tagname) {
        Log.i(TAG, "newTag : " + tagname);
        db.execSQL("CREATE TABLE " + tagname + "(origin_mac TEXT,last_mac TEXT, user TEXT,message BLOB,origin_date TEXT,origin_time TEXT,last_date TEXT,last_time TEXT,devices TEXT,hits INTEGER,TTL INTEGER,isImage INTEGER,isMine INTEGER)");
        db.execSQL("INSERT INTO Tags(tagname) VALUES ('" + tagname + "')");
    }

    public static Boolean existTag(SQLiteDatabase db, String tagname) {
        Cursor c = db.rawQuery("SELECT tagname FROM Tags WHERE tagname='" + tagname + "'", null);
        if (c.getCount() > 0) {
            Log.i(TAG, "TAG " + tagname + " EXISTS");
            c.close();
            return true;
        }
        c.close();
        return false;
    }

    /*
    INSERT A MESSAGE
    */
    public static void insertMessage(SQLiteDatabase db, BtMessage item) {
        Log.i(TAG,"insertMessage");
        String s;
        SQLiteStatement insertStatement;
        if (item.isGeneral) {
            s = "INSERT INTO Messages(" + datafields + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) ";
            insertStatement = db.compileStatement(s);
            insertStatement.bindString(1, item.origin_mac_address);
            insertStatement.bindString(2, item.last_mac_address);
            insertStatement.bindString(3, item.user);
            insertStatement.bindString(4, item.msg);
            insertStatement.bindString(5, item.origin_date);
            insertStatement.bindString(6, item.origin_time);
            insertStatement.bindString(7, item.last_date);
            insertStatement.bindString(8, item.last_time);
            insertStatement.bindString(9, "");          //Empty device List
            insertStatement.bindString(10, "0");
            insertStatement.bindString(11, String.valueOf(item.TTL));
            insertStatement.bindString(12, String.valueOf(item.isImage ? 1 : 0));
            insertStatement.bindString(13, String.valueOf(item.isMine ? 1 : 0));

        } else {
            // If the tag does not exists, create the tag
            if (!existTag(db, item.tag)) newTag(db, item.tag);

            s = "INSERT INTO " + item.tag + "(" + datafields + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) ";
            insertStatement = db.compileStatement(s);
            insertStatement.bindString(1, item.origin_mac_address);
            insertStatement.bindString(2, item.last_mac_address);
            insertStatement.bindString(3, item.user);
            insertStatement.bindString(4, item.msg);
            insertStatement.bindString(5, item.origin_date);
            insertStatement.bindString(6, item.origin_time);
            insertStatement.bindString(7, item.last_date);
            insertStatement.bindString(8, item.last_time);
            insertStatement.bindString(9, "");          //Empty device List
            insertStatement.bindString(10, "0");
            insertStatement.bindString(11, String.valueOf(item.TTL));
            insertStatement.bindString(12, String.valueOf(item.isImage ? 1 : 0));
            insertStatement.bindString(13, String.valueOf(item.isMine ? 1 : 0));

        }
        //Log.i(TAG, "insertMessage " + s);
        insertStatement.executeInsert();
    }

    /*
    RETURN A LIST OF MESSAGES FROM A TAG
     */
    public static ArrayList recoverMessagesByTag(SQLiteDatabase db, String Tag) {
        Log.i(TAG,"recoverMessagesByTag");

        ArrayList<BtMessage> messages = new ArrayList<>();
        //SELECT id,username,message,date,time FROM Messages WHERE hastag!='NONE'

        Cursor c = db.rawQuery("SELECT " + datafields + " FROM " + Tag, null);

        if (c.moveToFirst()) {
            do {
                // We use the constructor String mac,String msg,String user,String time,String date
                BtMessage item = new BtMessage(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getString(6), c.getString(7), c.getString(8), c.getInt(9), c.getInt(10), c.getInt(11));
                item.isMine = (c.getInt(12) != 0);
                item.setTag(Tag);   // Now we add the Tag
                messages.add(item);
                //Log.i(TAG,"RecoverMessagesByTag "+ item.toString());
            } while (c.moveToNext());
        }
        c.close();
        return messages;
    }

    public static ArrayList recoverLiveMessages(SQLiteDatabase db, int max) {
        Log.i(TAG,"recoverLiveMessages");
        ArrayList<BtMessage> messages = new ArrayList<>();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT " + datafields + " FROM Messages WHERE hits<" + String.valueOf(max), null);
            if (c.moveToFirst()) {
                do {

                    BtMessage item = new BtMessage(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getString(6), c.getString(7), c.getString(8), c.getInt(9), c.getInt(10), c.getInt(11));
                    item.isMine = (c.getInt(12) != 0);
                    messages.add(item);


                    //Log.i(TAG,"RecoverMessages: " + item.toString());
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error Accesing to this element");

        }
        c.close();

        ArrayList<String> tags = getTags(db);

        for (Object tag : tags) {
            c = db.rawQuery("SELECT " + datafields + " FROM " + tag + " WHERE hits<" + String.valueOf(max), null);

            if (c.moveToFirst()) {
                do {
                    // We use the constructor String mac,String msg,String user,String time,String date
                    try {
                        BtMessage item = new BtMessage(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getString(6), c.getString(7), c.getString(8), c.getInt(9), c.getInt(10), c.getInt(11));
                        item.isMine = (c.getInt(12) != 0);
                        item.setTag(tag.toString());   // Now we add the Tag
                        messages.add(item);
                    } catch (Exception e) {
                        Log.e(TAG, "Error Accesing to this element");
                    }

                } while (c.moveToNext());
            }
            c.close();
        }

        return messages;
    }

    public static ArrayList recoverMessages(SQLiteDatabase db) {
        Log.i(TAG,"recoverMessages");
        ArrayList<BtMessage> messages;
        messages = new ArrayList<>();
        //SELECT id,username,message,date,time FROM Messages WHERE hastag!='NONE'

        Cursor c = db.rawQuery("SELECT " + datafields + " FROM Messages", null);
        if (c.moveToFirst()) {
            do {
                try {
                    BtMessage item = new BtMessage(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getString(6), c.getString(7), c.getString(8), c.getInt(9), c.getInt(10), c.getInt(11));
                    item.isMine = (c.getInt(12) != 0);
                    messages.add(item);
                } catch (Exception e) {
                    Log.e(TAG, "Error Accesing to this element");
                }
                //Log.i(TAG, "RecoverMessages: " + item.toString());
            } while (c.moveToNext());
        }
        try{c.close();} catch(Exception e){}
        return messages;
    }


    public static Boolean isNew(SQLiteDatabase db, BtMessage item){
        ArrayList<BtMessage> messages;
        if(item.isGeneral){
            messages = recoverMessages(db);
            for(int i=0; i< messages.size(); i++){
                if(messages.get(i).toString().equals(item.toString())) return false;
            }
        }
        else{
            messages = recoverMessagesByTag(db, item.tag);
            for(int i=0; i< messages.size(); i++){
                if(messages.get(i).toString().equals(item.toString())) return false;
            }
        }
        return true;

    }

    public static void updateMessage(SQLiteDatabase db, BtMessage item) {
        Log.i(TAG,"updateMessage");
        BtMessage old_msg = getMessage(db, item.toHash());
        old_msg.addDevice(item.last_mac_address);

        if (old_msg != null) {
            ContentValues newValues = new ContentValues();
            newValues.put("devices", old_msg.devices.toString()); //These Fields should be your String values of actual column names
            newValues.put("last_mac", item.last_mac_address);
            newValues.put("last_date", item.last_date);
            newValues.put("last_time", item.last_time);
            newValues.put("hits", old_msg.hits +1);

            String[] whereArgs = {item.msg, item.origin_mac_address, item.origin_time, item.origin_date};
            if (item.isGeneral) {
                db.update("Messages", newValues, "message=? AND origin_mac=? AND origin_time=? AND origin_date=?", whereArgs);
            } else {
                db.update(item.tag, newValues, "message=? AND origin_mac=? AND origin_time=? AND origin_date=?", whereArgs);
            }
        }

    }

    public static void updateUsername(SQLiteDatabase db, String old_username, String new_username) {
        Log.i(TAG,"updateUsername");

        ContentValues newValues = new ContentValues();
        newValues.put("user", new_username); //These Fields should be your String values of actual column names
        String[] whereArgs = {old_username};

        db.update("Messages", newValues, "user=?", whereArgs);
        ArrayList<String> tags = getTags(db);

        for (String tag : tags) {
            db.update(tag, newValues, "user=?", whereArgs);
        }

    }


    public static void updateMessageDevices(SQLiteDatabase db, BtMessage item) {
        Log.i(TAG,"updateMessageDevices");

        BtMessage old_msg = getMessage(db, item.toHash());
        old_msg.addDevice(item.last_mac_address);

        if (old_msg != null) {
            ContentValues newValues = new ContentValues();
            newValues.put("devices", old_msg.devices.toString()); //These Fields should be your String values of actual column names
            newValues.put("last_mac", item.last_mac_address);
            newValues.put("last_date", item.last_date);
            newValues.put("last_time", item.last_time);
            String[] whereArgs = {item.msg, item.origin_mac_address, item.origin_time, item.origin_date};
            if (item.isGeneral) {
                db.update("Messages", newValues, "message=? AND origin_mac=? AND origin_time=? AND origin_date=?", whereArgs);
            } else {
                db.update(item.tag, newValues, "message=? AND origin_mac=? AND origin_time=? AND origin_date=?", whereArgs);
            }
        }
    }

    public static void updateMessageHits(SQLiteDatabase db, BtMessage item) {
        Log.i(TAG,"updateMessageHits");

        BtMessage old_msg = getMessage(db, item.toHash());
        ContentValues newValues = new ContentValues();
        newValues.put("hits", old_msg.hits +1);
        String[] whereArgs = {item.msg, item.origin_mac_address, item.origin_time, item.origin_date};
        if (item.isGeneral) {
            db.update("Messages", newValues, "message=? AND origin_mac=? AND origin_time=? AND origin_date=?", whereArgs);
        } else {
            db.update(item.tag, newValues, "message=? AND origin_mac=? AND origin_time=? AND origin_date=?", whereArgs);
        }

    }



    public static ArrayList getTags(SQLiteDatabase db) {
        Log.i(TAG,"getTags");

        // SELECT hastag FROM Messages WHERE hastag!='NONE'
        ArrayList<String> tags = new ArrayList<>();
        if (db != null) {
            Cursor c = db.rawQuery("SELECT tagname FROM Tags", null);

            //Nos aseguramos de que existe al menos un registro
            if (c.moveToFirst()) {
                //Recorremos el cursor hasta que no haya más registros
                do {
                    tags.add(c.getString(0));
                    Log.i(TAG, "Tag: " + c.getString(0));
                } while (c.moveToNext());
            }

            c.close();
        }
        return tags;
    }


    public static int getNumberOfEntriesByTag(SQLiteDatabase db,String tag){
        Log.i(TAG,"getNumberOfEntriesByTag");

        Cursor c = db.rawQuery("SELECT COUNT(origin_mac) FROM " + tag, null);
        c.moveToFirst();
        int n = c.getInt(0);
        c.close();
        return n;
    }

    public static void deleteTag(SQLiteDatabase db, String tagname){
        String s;

        s= "DROP TABLE "+ tagname;
        Log.i(TAG, s);
        db.execSQL("DROP TABLE " + tagname);

        s= "DELETE FROM Tags WHERE tagname ='" + tagname+"'";
        Log.i(TAG, s);
        db.execSQL(s);
    }

    public static BtMessage getMessage(SQLiteDatabase db, String hash) {

        Cursor c;

        c = db.rawQuery("SELECT " + datafields + " FROM Messages", null);
        if (c.moveToFirst()) {
            do {

                BtMessage item = new BtMessage(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getString(6), c.getString(7), c.getString(8), c.getInt(9), c.getInt(10), c.getInt(11));
                item.isMine = (c.getInt(12) != 0);
                if (item.toHash().equals(hash)) {
                    return item;
                }
            } while (c.moveToNext());
        }

        c.close();

        ArrayList<String> tags = getTags(db);
        for (Object tag : tags) {
            c = db.rawQuery("SELECT " + datafields + " FROM " + tag, null);

            if (c.moveToFirst()) {
                do {
                    // We use the constructor String mac,String msg,String user,String time,String date
                    BtMessage item = new BtMessage(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getString(6), c.getString(7), c.getString(8), c.getInt(9), c.getInt(10), c.getInt(11));
                    item.isMine = (c.getInt(12) != 0);
                    item.setTag(tag.toString());   // Now we add the Tag
                    if (item.toHash().equals(hash)) {
                        return item;
                    }
                } while (c.moveToNext());
            }
            c.close();
        }

        return null;
    }
    public static void deleteMessage(SQLiteDatabase db,BtMessage item){
        Log.i(TAG,"deleteMessage");

        String s;
        SQLiteStatement preparedStatement;
        if(item.isGeneral) {
            //s = "DELETE FROM Messages WHERE message='" + item.msg + "' AND id='" + item.origin_mac_address + "' AND time='" + item.origin_time + "' AND date='" + item.origin_time + "'";
            s = "DELETE FROM Messages WHERE message=? AND origin_mac=? AND origin_time=? AND origin_date=?";
            preparedStatement = db.compileStatement(s);
            preparedStatement.bindString(1, item.msg);
            preparedStatement.bindString(2, item.origin_mac_address);
            preparedStatement.bindString(3, item.origin_time);
            preparedStatement.bindString(4, item.origin_date);
            preparedStatement.executeUpdateDelete();
            Log.i(TAG, s);
            //db.execSQL(s);
        }else{
            //s = "DELETE FROM " + item.tag + " WHERE message='" + item.msg + "' AND id='" + item.origin_mac_address + "' AND time='" + item.origin_time + "' AND date='" + item.origin_date + "'";
            //Log.i(TAG,s);
            //db.execSQL(s);
            s = "DELETE FROM " + item.tag + " WHERE message=? AND origin_mac=? AND origin_time=? AND origin_date=?";
            preparedStatement = db.compileStatement(s);
            preparedStatement.bindString(1, item.msg);
            preparedStatement.bindString(2, item.origin_mac_address);
            preparedStatement.bindString(3, item.origin_time);
            preparedStatement.bindString(4, item.origin_date);
            preparedStatement.executeUpdateDelete();
            Log.i(TAG, s);
        }
    }

    public void onCreate(SQLiteDatabase db) {

        //db.execSQL("CREATE TABLE Messages(id TEXT, username TEXT,message TEXT,hastag TEXT,date TEXT,time TEXT)");
        db.execSQL("CREATE TABLE Tags(tagname TEXT)");
        db.execSQL("CREATE TABLE Messages(origin_mac TEXT,last_mac TEXT, user TEXT,message BLOB,origin_date TEXT,origin_time TEXT,last_date TEXT,last_time TEXT,devices TEXT,hits INTEGER,TTL INTEGER,isImage INTEGER,isMine INTEGER)");
    }

    public void onUpgrade(SQLiteDatabase db, int versionAnterior, int versionNueva) {

        // We remove the older version
        db.execSQL("DROP TABLE IF EXISTS Messages");

        //And create a new one
        //db.execSQL("CREATE TABLE Messages(id TEXT, username TEXT,message TEXT,hastag TEXT,date TEXT,time TEXT)");
        db.execSQL("CREATE TABLE Tags(tagname TEXT)");
        db.execSQL("CREATE TABLE Messages(origin_mac TEXT,last_mac TEXT, user TEXT,message TEXT,origin_date TEXT,origin_time TEXT,last_date TEXT,last_time TEXT,devices TEXT,hits INTEGER,TTL INTEGER,isImage INTEGER,isMine INTEGER)");
    }

}