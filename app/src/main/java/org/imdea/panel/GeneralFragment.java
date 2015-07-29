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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.imdea.panel.Database.BtMessage;
import org.imdea.panel.Database.DBHelper;
import org.imdea.panel.Services.mqtt.mqttService;
import org.imdea.panel.adapter.ItemAdapter;

import java.util.ArrayList;

@SuppressWarnings({"unchecked"})


public class GeneralFragment extends Fragment {

    public static ListView listv;
    public static Button send_btn;
    public static EditText text_field;
    public static ArrayList<BtMessage> messages;
    public static ItemAdapter adapter;
    final String TAG = "GeneralFragment";
    View rootView;
    SharedPreferences SP;

    public GeneralFragment(){

    }

    public static void refresh() {
        if (messages != null) {
            messages.clear();
            adapter.notifyDataSetInvalidated();
            messages.addAll(DBHelper.recoverMessages(Global.db));
            adapter.notifyDataSetChanged();
        }


    }

    public static boolean isBlank(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if ((Character.isWhitespace(str.charAt(i)) == false)) {
                return false;
            }
        }
        return true;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SP = PreferenceManager.getDefaultSharedPreferences(getActivity());


    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_general, container, false);

        messages = new ArrayList<>();
        messages = DBHelper.recoverMessages(Global.db);
            Log.i(TAG,"Showing General list");


        listv = (ListView) rootView.findViewById(R.id.listViewG);
        send_btn = (Button) rootView.findViewById(R.id.btnSend);
        text_field = (EditText) rootView.findViewById(R.id.inputMsg);

        send_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = text_field.getText().toString();
                text = text.replace("\n", " ");

                if (text.isEmpty()) return;
                if (isBlank(text)) return;

                BtMessage item = new BtMessage(text, SP.getString("username_field", "anonymous"));
                item.isMine = true;
                Log.i(TAG, "NEW MSG" + text);
                DBHelper.insertMessage(Global.db, item);
                text_field.setText("");

                item.last_mac_address = Global.DEVICE_ADDRESS;

                if (Global.mqtt) mqttService.sendToPeers(item);

                InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                refresh();
            }
        });

        adapter = new ItemAdapter(getActivity(), messages) {
            public void onEntrada(Object item, View view) {
                BtMessage Btitem = (BtMessage) item;

                TextView text_msg = (TextView) view.findViewById(R.id.msg);
                text_msg.setText(Btitem.msg);

                TextView text_user = (TextView) view.findViewById(R.id.name);
                text_user.setText(Btitem.user);

            }
        };

        listv.setAdapter(adapter);

        listv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object listItem = listv.getItemAtPosition(position);
                Log.i(TAG, "Click! " + listItem.toString());
            }
        });

        listv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final BtMessage listItem = (BtMessage) listv.getItemAtPosition(position);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                final CharSequence[] shareItems = {"Delete", "Information", "Share"};
                builder.setCancelable(true).setItems(shareItems, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (id == 0) {
                            DBHelper.deleteMessage(Global.db, listItem);
                            refresh();
                        } else if (id == 1) {
                            Intent intent = new Intent(getActivity(), InfoActivity.class);
                            intent.putExtra("HASH", listItem.toHash());
                            startActivity(intent);
                        } else {

                            Intent sendIntent = new Intent();
                            sendIntent.setAction(Intent.ACTION_SEND);
                            sendIntent.putExtra(Intent.EXTRA_TEXT, "This is my text to send.");
                            sendIntent.setType("text/plain");
                            startActivity(Intent.createChooser(sendIntent, "Share"));
                        }

                    }
                });

                AlertDialog alert = builder.create();
                alert.show();
                return false;
            }

        });

        return rootView;
    }


    public void onDetach() {

        super.onDetach();
    }

}
