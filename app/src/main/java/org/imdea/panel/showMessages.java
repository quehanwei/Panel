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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.imdea.panel.Database.BtMessage;
import org.imdea.panel.Database.DBHelper;
import org.imdea.panel.Services.mqttService;
import org.imdea.panel.adapter.ItemAdapter;

import java.util.ArrayList;


public class showMessages extends FragmentActivity {

    public static ListView listv;
    public static Button send_btn;
    public static EditText text_field;
    public static ArrayList<BtMessage> messages;
    public static String tag;
    public static ItemAdapter adapter;
    public static SharedPreferences SP;
    public FragmentManager fm;

    public static void refresh() {
        messages.clear();
        messages.addAll(DBHelper.recoverMessagesByTag(Global.db, tag));
        adapter.notifyDataSetChanged();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_messages);

        tag = getIntent().getExtras().getString("tag");
        messages = DBHelper.recoverMessagesByTag(Global.db, tag);
        setTitle(tag);
        fm = getFragmentManager();
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(true);
        SP = PreferenceManager.getDefaultSharedPreferences(this);

        listv = (ListView) findViewById(R.id.listViewM);
        send_btn = (Button) findViewById(R.id.btnSend);
        text_field = (EditText) findViewById(R.id.inputMsg);

        send_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = text_field.getText().toString();
                if (!text.isEmpty()) {
                    text = text.replace("\n", " ");
                    BtMessage item = new BtMessage(text, SP.getString("username_field", "anonymous"));
                    item.isMine = true;
                    item.setTag(tag);
                    DBHelper.insertMessage(Global.db, item);
                    text_field.setText("");
                    mqttService.sendToPeers(item);

                    TagsFragment.refresh();

                }
                InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                refresh();
            }
        });

        adapter = new ItemAdapter(this, messages) {
            public void onEntrada(Object item, View view) {
                TextView text_msg = (TextView) view.findViewById(R.id.msg);
                text_msg.setText(((BtMessage) item).msg);
                TextView text_user = (TextView) view.findViewById(R.id.name);
                text_user.setText(((BtMessage) item).user);
            }
        };

        listv.setAdapter(adapter);

        listv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final BtMessage listItem = (BtMessage) listv.getItemAtPosition(position);
                Log.i("TAG_FRAGMENT", "Long Click on " + listItem.toString());
                AlertDialog.Builder builder = new AlertDialog.Builder(showMessages.this);
                final CharSequence[] shareItems = {"Delete", "Information"};
                builder.setCancelable(true).setItems(shareItems, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (id == 0) {
                            DBHelper.deleteMessage(Global.db, listItem);
                            //Messages.deleteMessage(listItem);
                            refresh();
                        } else {
                            Intent intent = new Intent(getBaseContext(), InfoActivity.class);
                            intent.putExtra("HASH", listItem.toHash());
                            startActivity(intent);
                        }
                    }
                });

                /*builder.setCancelable(true).setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        DBHelper.deleteMessage(MainActivity.db,(BtMessage) listItem);
                        refresh();
                    }
                });*/

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
        switch (item.getItemId()) {
            case R.id.action_newMsg:
                Bundle args = new Bundle();
                args.putBoolean("isTag", false);
                args.putString("Tag", tag);
                InputFragment newMsgDialog = new InputFragment();
                newMsgDialog.setArguments(args);
                newMsgDialog.show(getSupportFragmentManager(), "fragment_edit_name");
                return true;

            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);

    }
}
