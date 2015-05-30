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

/**
 * This class does all the work with the messages database:
 *
 * newTag
 * TagExists
 * insertMessage
 * RecoverMessagesByTag
 * RecoverMessages
 * getTags
 * getNumberOfEntriesByTag
 * deleteTag
 * deleteMessage
 *
 */

/* TODO
    Avoid sql Injection attacks -- > Process strings with special characters

 */
public class DBHelper extends SQLiteOpenHelper {

    private static final String TAG = "DBHelper";

    public DBHelper(Context contexto, String nombre, SQLiteDatabase.CursorFactory factory, int version) {

        super(contexto, nombre, factory, version);

    }

    public static void newTag(SQLiteDatabase db, String tagname) {
        Log.i(TAG, "newTag : " + tagname);
        db.execSQL("CREATE TABLE " + tagname + "(origin_mac TEXT,last_mac TEXT, user TEXT,message TEXT,origin_date TEXT,origin_time TEXT,last_date TEXT,last_time TEXT,devices TEXT,hits INTEGER)");
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
        String s;
        SQLiteStatement insertStatement;
        if (item.isGeneral) {
            s = "INSERT INTO Messages(origin_mac, last_mac, user, message, origin_date, origin_time, last_date, last_time, devices, hits) VALUES (?,?,?,?,?,?,?,?,?,?) ";
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
        } else {
            // If the tag does not exists, create the tag
            if (!existTag(db, item.tag)) newTag(db, item.tag);

            s = "INSERT INTO " + item.tag + "(origin_mac, last_mac, user, message, origin_date, origin_time, last_date, last_time, devices, hits) VALUES (?,?,?,?,?,?,?,?,?,?) ";
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
            Log.i(TAG, "insertMessage " + insertStatement.toString());

        }
        //Log.i(TAG, "insertMessage " + s);
        insertStatement.executeInsert();
    }

    /*
    RETURN A LIST OF MESSAGES FROM A TAG
     */
    public static ArrayList recoverMessagesByTag(SQLiteDatabase db, String Tag) {

        ArrayList<BtMessage> messages = new ArrayList<>();
        //SELECT id,username,message,date,time FROM Messages WHERE hastag!='NONE'

        Cursor c = db.rawQuery("SELECT origin_mac, last_mac, user, message, origin_date, origin_time, last_date, last_time, devices, hits FROM " + Tag, null);

        if (c.moveToFirst()) {
            do {
                // We use the constructor String mac,String msg,String user,String time,String date
                BtMessage item = new BtMessage(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getString(6), c.getString(7), c.getString(8), c.getInt(9));
                item.setTag(Tag);   // Now we add the Tag
                messages.add(item);
                Log.i(TAG,"RecoverMessagesByTag "+ item.toString());
            } while (c.moveToNext());
        }
        c.close();
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
        ContentValues newValues = new ContentValues();
        newValues.put("devices", item.devices.toString()); //These Fields should be your String values of actual column names
        newValues.put("hits", item.hits);
        String[] whereArgs = {item.msg, item.origin_mac_address, item.origin_time, item.origin_date};
        if (item.isGeneral) {
            db.update("Messages", newValues, "message=? AND origin_mac=? AND origin_time=? AND origin_date=?", whereArgs);
        } else {
            db.update(item.tag, newValues, "message=? AND origin_mac=? AND origin_time=? AND origin_date=?", whereArgs);
        }
    }

    public static ArrayList recoverMessages(SQLiteDatabase db) {
        ArrayList<BtMessage> messages;
        messages = new ArrayList<>();
        //SELECT id,username,message,date,time FROM Messages WHERE hastag!='NONE'

        Cursor c = db.rawQuery("SELECT origin_mac, last_mac, user, message, origin_date, origin_time, last_date, last_time, devices, hits FROM Messages", null);
        if (c.moveToFirst()) {
            do {

                BtMessage item = new BtMessage(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getString(6), c.getString(7), c.getString(8), c.getInt(9));
                messages.add(item);
                //Log.i(TAG,"RecoverMessages: " + item.toString());
            } while (c.moveToNext());
        }
        c.close();
        return messages;
    }

    public static ArrayList getTags(SQLiteDatabase db) {
        // SELECT hastag FROM Messages WHERE hastag!='NONE'
        ArrayList<String> tags = new ArrayList<>();

        Cursor c = db.rawQuery("SELECT tagname FROM Tags", null);

        //Nos aseguramos de que existe al menos un registro
        if (c.moveToFirst()) {
            //Recorremos el cursor hasta que no haya más registros
            do {

                tags.add(c.getString(0));
                Log.i(TAG,"Tag: " + c.getString(0));
            } while (c.moveToNext());
        }

        c.close();

        return tags;
    }

    public static int getNumberOfEntriesByTag(SQLiteDatabase db,String tag){
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

    public static void deleteMessage(SQLiteDatabase db,BtMessage item){
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

    public static ArrayList recoverLiveMessages(SQLiteDatabase db, int max) {
        ArrayList<BtMessage> messages = new ArrayList<>();
        Cursor c;

        c = db.rawQuery("SELECT origin_mac, last_mac, user, message, origin_date, origin_time, last_date, last_time, devices, hits FROM Messages WHERE hits<" + String.valueOf(max), null);
        if (c.moveToFirst()) {
            do {

                BtMessage item = new BtMessage(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getString(6), c.getString(7), c.getString(8), c.getInt(9));
                messages.add(item);
                //Log.i(TAG,"RecoverMessages: " + item.toString());
            } while (c.moveToNext());
        }
        c.close();

        ArrayList<String> tags = getTags(db);
        for (Object tag : tags) {
            c = db.rawQuery("SELECT origin_mac, last_mac, user, message, origin_date, origin_time, last_date, last_time, devices, hits FROM " + tag + " WHERE hits<" + String.valueOf(max), null);

            if (c.moveToFirst()) {
                do {
                    // We use the constructor String mac,String msg,String user,String time,String date
                    BtMessage item = new BtMessage(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getString(6), c.getString(7), c.getString(8), c.getInt(9));
                    item.setTag(tag.toString());   // Now we add the Tag
                    messages.add(item);
                } while (c.moveToNext());
            }
            c.close();
        }

        return messages;
    }

    public void onCreate(SQLiteDatabase db) {
        //db.execSQL("CREATE TABLE Messages(id TEXT, username TEXT,message TEXT,hastag TEXT,date TEXT,time TEXT)");
        db.execSQL("CREATE TABLE Tags(tagname TEXT)");
        db.execSQL("CREATE TABLE Messages(origin_mac TEXT,last_mac TEXT, user TEXT,message TEXT,origin_date TEXT,origin_time TEXT,last_date TEXT,last_time TEXT,devices TEXT,hits INTEGER)");
    }

    public void onUpgrade(SQLiteDatabase db, int versionAnterior, int versionNueva) {

        // We remove the older version
        db.execSQL("DROP TABLE IF EXISTS Messages");

        //And create a new one
        //db.execSQL("CREATE TABLE Messages(id TEXT, username TEXT,message TEXT,hastag TEXT,date TEXT,time TEXT)");
        db.execSQL("CREATE TABLE Tags(tagname TEXT)");
        db.execSQL("CREATE TABLE Messages(origin_mac TEXT,last_mac TEXT, user TEXT,message TEXT,origin_date TEXT,origin_time TEXT,last_date TEXT,last_time TEXT,devices TEXT,hits INTEGER)");
    }

}