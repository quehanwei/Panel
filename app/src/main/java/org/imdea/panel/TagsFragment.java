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

import org.imdea.panel.adapter.ItemAdapter;

import java.util.ArrayList;

public class TagsFragment extends Fragment {

    public static ListView listv;
    View rootView;
    public static ArrayList<String> tags;
    public static ItemAdapter adapter;
    public TagsFragment(){

    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tags = DBHelper.getTags(MainActivity.db);

    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_tags, container, false);

        listv = (ListView) rootView.findViewById(R.id.listViewT);

        adapter = new ItemAdapter(getActivity(), R.layout.row_layout, tags){
            @Override
            public void onEntrada(Object item, View view) {
                if (item != null) {
                    TextView text_msg = (TextView) view.findViewById(R.id.name);
                    text_msg.setText("#" +(String) item);
                    TextView text_datetime = (TextView) view.findViewById(R.id.datetime);
                    text_datetime.setText(DBHelper.getNumberOfEntriesByTag(MainActivity.db,(String)item)+" messages");
                }
            }
        };

        listv.setAdapter(adapter);

        listv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Object listItem = listv.getItemAtPosition(position);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setCancelable(true).setPositiveButton("UNSUSCRIBE", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        DBHelper.deleteTag(MainActivity.db, listItem.toString());
                        refresh();
                    }
                });

                AlertDialog alert = builder.create();
                alert.show();
                return true;
            }

        });

        listv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object listItem = listv.getItemAtPosition(position);

                Log.i("TAG_FRAGMENT", "CLICK ON " + listItem.toString());
                Intent intent = new Intent(getActivity(), showMessages.class);
                Bundle b = new Bundle();
                b.putString("tag", listItem.toString()); //Your id
                intent.putExtras(b); //Put your id to your next Intent
                getActivity().startActivity(intent);
            }
        });


        return rootView;
    }

    public static void refresh(){
        tags.clear();
        tags.addAll(DBHelper.getTags(MainActivity.db));
        adapter.notifyDataSetChanged();
        //listv.setAdapter(adapter);
    }

}
