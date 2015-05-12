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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class InputFragment extends DialogFragment {

    Button btn;
    String Tag;
    SharedPreferences SP;
    private EditText mEditText;
    private Boolean isTag;
    public InputFragment() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_input, container);

        isTag = getArguments().getBoolean("isTag");
        Tag = getArguments().getString("Tag");

        mEditText = (EditText) view.findViewById(R.id.txt_your_name);
        btn = (Button) view.findViewById(R.id.send_btn);

        // We set the title of the box
        if (isTag) {
            getDialog().setTitle("New Tag");
            btn.setText("Add");
        }

        else getDialog().setTitle("New Message");

        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SP = PreferenceManager.getDefaultSharedPreferences(getActivity());
                String inputText = mEditText.getText().toString();
                if(inputText.isEmpty()){
                    Toast toast = Toast.makeText(getActivity().getApplicationContext(), "You didnt write anything!", Toast.LENGTH_SHORT);
                    toast.show();
                    dismiss();
                }
                else {
                    if (isTag) {                 // IF THE STRING IS A TAG

                        if (inputText.indexOf(' ') != -1) {
                            Toast toast = Toast.makeText(getActivity().getApplicationContext(), "You cannot use whitespaces on a Tag", Toast.LENGTH_SHORT);
                            toast.show();
                        } else {
                            if (!DBHelper.TagExists(MainActivity.db, mEditText.getText().toString())) {    // If the tag does not exists
                                DBHelper.newTag(MainActivity.db, inputText);
                                TagsFragment.refresh();
                            } else {
                                Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Tag already exists", Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        }

                    } else {


                        if (Tag != null) {           // If the message belongs to a tag
                            BtMessage item = new BtMessage(inputText, SP.getString("username_field", "anonymous"));
                            item.setTag(Tag);
                            DBHelper.insertMessage(MainActivity.db, item);
                            showMessages.refresh();
                            MainActivity.mChatService.addMessage(item);
                            MainActivity.mChatService.startDiscovery();

                        } else {                      // If the message is general
                            BtMessage item = new BtMessage(inputText, SP.getString("username_field", "anonymous"));
                            DBHelper.insertMessage(MainActivity.db, item);
                            GeneralFragment.refresh();
                            MainActivity.mChatService.addMessage(item);
                            MainActivity.mChatService.startDiscovery();

                        }

                    }
                }
                dismiss();

            }
        });

        return view;
    }


}