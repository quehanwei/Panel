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
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import org.imdea.panel.Database.BtMessage;
import org.imdea.panel.Database.DBHelper;
import org.imdea.panel.Database.Messages;
import org.imdea.panel.adapter.ItemAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class GeneralFragment extends Fragment {

    public static ListView listv;
    public static ArrayList<BtMessage> messages;
    public static ItemAdapter adapter;
    final String TAG = "GeneralFragment";
    View rootView;
    public GeneralFragment(){

    }

    public static void refresh() {
        messages.clear();
        messages.addAll(DBHelper.recoverMessages(MainActivity.db));
        adapter.notifyDataSetChanged();

    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_general, container, false);

        messages = new ArrayList<>();
        messages = DBHelper.recoverMessages(MainActivity.db);
            Log.i(TAG,"Showing General list");


        listv = (ListView) rootView.findViewById(R.id.listViewG);

        adapter = new ItemAdapter(getActivity(), R.layout.row_layout, messages) {


            @Override


            public void onEntrada(Object item, View view) {
                if (item != null) {
                    // Changen the colour to easily know the differences between your messages and my messages
                    if (((BtMessage) item).origin_mac_address.equals(BluetoothAdapter.getDefaultAdapter().getAddress()))
                        view.setBackgroundColor(0xCDCDCD);
                    else
                        Log.w(TAG, ((BtMessage) item).origin_mac_address + "!=" + BluetoothAdapter.getDefaultAdapter().getAddress());

                    //Log.i(TAG,"New Item " + item.toString());
                    TextView text_msg = (TextView) view.findViewById(R.id.msg);
                    text_msg.setText(((BtMessage) item).msg);
                    TextView text_user = (TextView) view.findViewById(R.id.name);
                    text_user.setText(((BtMessage) item).user);
                    TextView text_datetime = (TextView) view.findViewById(R.id.datetime);
                    if (((BtMessage) item).last_date.equals(new SimpleDateFormat("MM.dd.yyyy").format(new Date())))
                        text_datetime.setText("Today at " + ((BtMessage) item).last_time);
                    else{
                        text_datetime.setText(((BtMessage) item).last_date + " at " + ((BtMessage) item).last_time);
                    }

                }
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

                final CharSequence[] shareItems = {"Delete", "Information"};
                builder.setCancelable(true).setItems(shareItems, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (id == 0) {
                            DBHelper.deleteMessage(MainActivity.db, listItem);
                            Messages.deleteMessage(listItem);
                            refresh();
                        } else {
                            Intent intent = new Intent(getActivity(), InfoActivity.class);
                            intent.putExtra("HASH", listItem.toHash());
                            startActivity(intent);
                        }

                    }
                });

                /*
                builder.setCancelable(true).setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                DBHelper.deleteMessage(MainActivity.db,(BtMessage) listItem);
                                refresh();
                            }
                        });
                */
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
