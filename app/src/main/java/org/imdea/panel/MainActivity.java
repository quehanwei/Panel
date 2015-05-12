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

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import org.imdea.panel.adapter.TabsPagerAdapter;

public class MainActivity extends FragmentActivity implements ActionBar.TabListener {

    public static FragmentManager fm;
    public static SQLiteDatabase db;
    public static BluetoothChatService mChatService = null;
    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    break;
                case Constants.MESSAGE_WRITE:   // I'm sending the message
                    break;
                case Constants.MESSAGE_READ:    // I'm receiving the Message
                    GeneralFragment.refresh();
                    TagsFragment.refresh();
                    break;
                case Constants.MESSAGE_TOAST:
                    break;
            }
        }
    };
    private ViewPager viewPager;
    private ActionBar actionbar;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Initilization
        viewPager = (ViewPager) findViewById(R.id.pager);
        actionbar = getActionBar();
        TabsPagerAdapter mAdapter = new TabsPagerAdapter(getSupportFragmentManager());
        fm = getSupportFragmentManager();

        viewPager.setAdapter(mAdapter);
        actionbar.setHomeButtonEnabled(false);
        actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        actionbar.addTab(actionbar.newTab().setText("General").setTabListener(this));
        actionbar.addTab(actionbar.newTab().setText("Tags").setTabListener(this));

        /**
         * on swiping the viewpager make respective tab selected
         * */
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                // on changing the page
                // make respected tab selected
                actionbar.setSelectedNavigationItem(position);
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
            }
        });

        //Abrimos la base de datos 'DBUsuarios' en modo escritura
        DBHelper msg_database = new DBHelper(this, "messages.db", null, 1);
        db = msg_database.getWritableDatabase();
        // Initialize the BluetoothChatService to perform bluetooth connections
        if (mChatService == null) mChatService = new BluetoothChatService(this, mHandler);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        Bundle args;

        switch (item.getItemId()){

            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;

            case R.id.action_newGrp:
                args = new Bundle();
                args.putBoolean("isTag",true);
                InputFragment newTagDialog = new InputFragment();
                newTagDialog.setArguments(args);
                newTagDialog.show(fm, "fragment_edit_name");
                return true;

            case R.id.action_newMsg:
                //FragmentManager fm = getSupportFragmentManager();
                args = new Bundle();
                args.putBoolean("isTag",false);
                InputFragment newMsgDialog = new InputFragment();
                newMsgDialog.setArguments(args);
                newMsgDialog.show(fm, "fragment_edit_name");
                return true;

            case R.id.action_exit:
                finish();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
        if (mChatService == null) mChatService = new BluetoothChatService(this, mHandler);

    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }

    protected void onDestroy(Bundle savedInstance) {
        db.close();
        //FtpUpload uploadData = new FtpUpload();
        //mChatService.stop();

    }

}
