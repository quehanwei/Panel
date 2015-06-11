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

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.imdea.panel.Bluetooth.Global;
import org.imdea.panel.Database.DBHelper;


public class InputFragment extends DialogFragment {

    Button btn;
    String Tag;
    private EditText mEditText;
    public InputFragment() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_input, container);

        Tag = getArguments().getString("Tag");

        mEditText = (EditText) view.findViewById(R.id.txt_your_name);
        btn = (Button) view.findViewById(R.id.send_btn);

        getDialog().setTitle("New Tag");

        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                String inputText = mEditText.getText().toString();

                if(inputText.isEmpty()){
                    Toast toast = Toast.makeText(getActivity().getApplicationContext(), "You didnt write anything!", Toast.LENGTH_SHORT);
                    toast.show();
                    dismiss();
                } else {
                        if (inputText.indexOf(' ') != -1) {
                            Toast toast = Toast.makeText(getActivity().getApplicationContext(), "You cannot use whitespaces on a Tag", Toast.LENGTH_SHORT);
                            toast.show();
                        } else {
                            if (!DBHelper.existTag(Global.db, mEditText.getText().toString())) {    // If the tag does not exists
                                DBHelper.newTag(Global.db, inputText);
                                TagsFragment.refresh();
                            } else {
                                Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Tag already exists", Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        }
                }
                dismiss();

            }
        });

        return view;
    }


}