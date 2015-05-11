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

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import org.imdea.panel.adapter.ItemAdapter;

import java.util.ArrayList;


public class showMessages extends FragmentActivity {

    public static ListView listv;
    View rootView;
    public static ArrayList<BtMessage> messages;
    public static String tag;
    public static ItemAdapter adapter;
    public FragmentManager fm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_messages);

        tag = getIntent().getExtras().getString("tag");
        messages = DBHelper.RecoverMessagesByTag(MainActivity.db,tag);
        setTitle(tag);
        listv = (ListView) findViewById(R.id.listViewM);
        fm = getFragmentManager();

        adapter = new ItemAdapter(this, R.layout.row_layout, messages){
            public void onEntrada(Object item, View view) {
                if (item != null) {
                    TextView text_msg = (TextView) view.findViewById(R.id.msg);
                    text_msg.setText(((BtMessage) item).msg);
                    TextView text_user = (TextView) view.findViewById(R.id.name);
                    text_user.setText(((BtMessage) item).user);
                    TextView text_datetime = (TextView) view.findViewById(R.id.datetime);
                    text_datetime.setText(((BtMessage) item).date +" "+ ((BtMessage) item).time);
                }
            }
        };

        listv.setAdapter(adapter);

        listv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Object listItem = listv.getItemAtPosition(position);
                Log.i("TAG_FRAGMENT", "Long Click on " + listItem.toString());
                AlertDialog.Builder builder = new AlertDialog.Builder(showMessages.this);
                builder.setCancelable(true).setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        DBHelper.deleteMessage(MainActivity.db,(BtMessage) listItem);
                        refresh();
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
                return false;
            }

        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_show_messages, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.action_newMsg){
            Bundle args = new Bundle();
            args.putBoolean("isTag",false);
            args.putString("Tag",tag);
            InputFragment newMsgDialog = new InputFragment();
            newMsgDialog.setArguments(args);
            newMsgDialog.show(getSupportFragmentManager(),"fragment_edit_name");
        }

        return super.onOptionsItemSelected(item);
    }

    public static void refresh(){
        messages.clear();
        messages.addAll(DBHelper.RecoverMessagesByTag(MainActivity.db,tag));
        adapter.notifyDataSetChanged();

    }
}
