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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
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
    Avoid sql Injection attacks
    Process strings with special characters

 */
public class DBHelper extends SQLiteOpenHelper {

    private static final String TAG = "DBHelper";

    public DBHelper(Context contexto, String nombre, SQLiteDatabase.CursorFactory factory, int version) {

        super(contexto, nombre, factory, version);

    }

    public static void newTag(SQLiteDatabase db, String tagname) {
        Log.i(TAG, "newTag : " + tagname);
        db.execSQL("CREATE TABLE " + tagname + "(id TEXT, username TEXT,message TEXT,date TEXT,time TEXT)");
        db.execSQL("INSERT INTO Tags(tagname) VALUES ('" + tagname + "')");
    }

    public static Boolean TagExists(SQLiteDatabase db, String tagname) {
        Cursor c = db.rawQuery("SELECT tagname FROM Tags WHERE tagname='" + tagname + "'", null);
        if (c.getCount() > 0) {
            Log.i(TAG, "TAG " + tagname + " EXISTS");
            return true;
        }
        return false;
    }

    /*
    INSERT A MESSAGE
    */
    public static void insertMessage(SQLiteDatabase db, BtMessage item) {
        String s;
        if (item.isGeneral) {
            s = "INSERT INTO Messages(id, username ,message ,date ,time ) ";
            s = s + "VALUES ('" + item.mac_address + "', '" + item.user + "', '" + item.msg + "', '" + item.date + "', '" + item.time + "')";
            Log.i(TAG, "insertMessage " + s);
        } else {
            if (!TagExists(db, item.tag)) newTag(db, item.tag);
            s = "INSERT INTO " + item.tag + "(id, username ,message ,date ,time ) ";
            s = s + "VALUES ('" + item.mac_address + "', '" + item.user + "', '" + item.msg + "', '" + item.date + "', '" + item.time + "')";
            Log.i(TAG, "insertTagMessage " + s);

        }
        db.execSQL(s);
    }

    /*
    RETURN A LIST OF MESSAGES FROM A TAG
     */
    public static ArrayList RecoverMessagesByTag(SQLiteDatabase db, String Tag) {

        ArrayList<BtMessage> messages = new ArrayList<BtMessage>();
        //SELECT id,username,message,date,time FROM Messages WHERE hastag!='NONE'

        Cursor c = db.rawQuery("SELECT id,username,message,date,time FROM " + Tag, null);
        if (c.moveToFirst()) {
            do {

                // We use the constructor String mac,String msg,String user,String time,String date
                BtMessage item = new BtMessage(c.getString(0), c.getString(2), c.getString(1), c.getString(4), c.getString(3));
                item.setTag(Tag);   // Now we add the Tag
                messages.add(item);
                Log.i(TAG,"RecoverMessagesByTag "+ item.toString());
            } while (c.moveToNext());
        }
        return messages;
    }

    public static Boolean isNew(SQLiteDatabase db, BtMessage item){
        ArrayList<BtMessage> messages;
        if(item.isGeneral){
          messages = RecoverMessages(db);
            for(int i=0; i< messages.size(); i++){
                if(messages.get(i).toString().equals(item.toString())) return false;
            }
        }
        else{
            messages = RecoverMessagesByTag(db,item.tag);
            for(int i=0; i< messages.size(); i++){
                if(messages.get(i).toString().equals(item.toString())) return false;
            }
        }
        return true;

    }

    public static ArrayList RecoverMessages(SQLiteDatabase db) {
        ArrayList<BtMessage> messages;
        messages = new ArrayList<BtMessage>();
        //SELECT id,username,message,date,time FROM Messages WHERE hastag!='NONE'

        Cursor c = db.rawQuery("SELECT id,message,username,time,date FROM Messages", null);
        if (c.moveToFirst()) {
            do {
                BtMessage item = new BtMessage(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4));
                messages.add(item);
                Log.i(TAG,"RecoverMessages: " + item.toString());
            } while (c.moveToNext());
        }
        return messages;
    }

    public static ArrayList getTags(SQLiteDatabase db) {
        // SELECT hastag FROM Messages WHERE hastag!='NONE'
        ArrayList<String> tags = new ArrayList<String>();

        Cursor c = db.rawQuery("SELECT tagname FROM Tags", null);

        //Nos aseguramos de que existe al menos un registro
        if (c.moveToFirst()) {
            //Recorremos el cursor hasta que no haya más registros
            do {

                tags.add(c.getString(0));
                Log.i(TAG,"Tag: " + c.getString(0));
            } while (c.moveToNext());
        }

        return tags;
    }

    public static int getNumberOfEntriesByTag(SQLiteDatabase db,String tag){
        Cursor c = db.rawQuery("SELECT COUNT(id) FROM "+tag, null);
        c.moveToFirst();
        return c.getInt(0);
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
        if(item.isGeneral) {
            s = "DELETE FROM Messages WHERE message='" + item.msg + "' AND id='" + item.mac_address + "' AND time='" + item.time + "' AND date='" + item.date + "'";
            Log.i(TAG, s);
            db.execSQL(s);
        }else{
            s = "DELETE FROM "+ item.tag +" WHERE message='" + item.msg + "' AND id='" +item.mac_address +"' AND time='"+ item.time +"' AND date='"+ item.date+"'";
            Log.i(TAG,s);
            db.execSQL(s);
        }
    }

    public void onCreate(SQLiteDatabase db) {
        //db.execSQL("CREATE TABLE Messages(id TEXT, username TEXT,message TEXT,hastag TEXT,date TEXT,time TEXT)");
        db.execSQL("CREATE TABLE Tags(tagname TEXT)");
        db.execSQL("CREATE TABLE Messages(id TEXT, username TEXT,message TEXT,date TEXT,time TEXT)");
    }

    public void onUpgrade(SQLiteDatabase db, int versionAnterior, int versionNueva) {

        // We remove the older version
        db.execSQL("DROP TABLE IF EXISTS Messages");

        //And create a new one
        //db.execSQL("CREATE TABLE Messages(id TEXT, username TEXT,message TEXT,hastag TEXT,date TEXT,time TEXT)");
        db.execSQL("CREATE TABLE Tags(tagname TEXT)");
        db.execSQL("CREATE TABLE Messages(id TEXT, username TEXT,message TEXT,date TEXT,time TEXT)");
    }

}