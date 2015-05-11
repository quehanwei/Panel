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
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import org.imdea.panel.adapter.ItemAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class GeneralFragment extends Fragment {

    public static ListView listv;
    View rootView;
    public static ArrayList<BtMessage> messages;
    String tag = null;
    public static ItemAdapter adapter;
    final String TAG = "GeneralFragment";
    public GeneralFragment(){

    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_general, container, false);

            messages = new ArrayList<BtMessage>();
            messages = DBHelper.RecoverMessages(MainActivity.db);
            Log.i(TAG,"Showing General list");


        listv = (ListView) rootView.findViewById(R.id.listViewG);

        adapter = new ItemAdapter(getActivity(), R.layout.row_layout, messages) {
            @Override
            public void onEntrada(Object item, View view) {
                if (item != null) {

                    //Log.i(TAG,"New Item " + item.toString());
                    TextView text_msg = (TextView) view.findViewById(R.id.msg);
                    text_msg.setText(((BtMessage) item).msg);
                    TextView text_user = (TextView) view.findViewById(R.id.name);
                    text_user.setText(((BtMessage) item).user);
                    TextView text_datetime = (TextView) view.findViewById(R.id.datetime);
                    if(((BtMessage) item).date.equals(new SimpleDateFormat("MM.dd.yyyy").format(new Date())))   text_datetime.setText( "Today at " + ((BtMessage) item).time);
                    else{
                        text_datetime.setText(((BtMessage) item).date + " at " + ((BtMessage) item).time);
                    }

                }
            }
        };

        listv.setAdapter(adapter);

        listv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object listItem = listv.getItemAtPosition(position);
                Log.i(TAG,"Click! "+listItem.toString());
            }
        });

        listv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Object listItem = listv.getItemAtPosition(position);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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

        return rootView;
    }

    public static void refresh(){
        messages.clear();
        messages.addAll(DBHelper.RecoverMessages(MainActivity.db));
        adapter.notifyDataSetChanged();

    }


    public void onDetach() {
        super.onDetach();
    }

}
