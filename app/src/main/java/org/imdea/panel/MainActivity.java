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
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewConfiguration;

import com.parse.Parse;
import com.parse.ParseObject;

import org.imdea.panel.Database.BtMessage;
import org.imdea.panel.Database.DBHelper;
import org.imdea.panel.Services.Bluetooth.BtService;
import org.imdea.panel.Services.BtMesh.MeshService;
import org.imdea.panel.Services.BtWiz.BtWizService;
import org.imdea.panel.Services.Wifi.WifiService;
import org.imdea.panel.Services.infraestructService.commService;
import org.imdea.panel.Services.mqtt.mqttService;
import org.imdea.panel.adapter.TabsPagerAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;

@SuppressWarnings({"unchecked", "deprecation"})

public class MainActivity extends FragmentActivity implements ActionBar.TabListener {

    public static FragmentManager fm;
    SharedPreferences SP;
    private newMessage messageReceiver = new newMessage();

    private ViewPager viewPager;
    private android.app.ActionBar actionbar;


    private Thread.UncaughtExceptionHandler onRuntimeError = new Thread.UncaughtExceptionHandler() {
        public void uncaughtException(Thread thread, Throwable ex) {
            ex.printStackTrace();
            //Try starting the Activity again
        }

    };

    public MainActivity() {

    }
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
                Log.e("Alert", "UNCAUGHT EXCEPTION", paramThrowable);
                System.exit(1);
            }
        });
        Thread.setDefaultUncaughtExceptionHandler(onRuntimeError);


        /*
        ParseObject testObject = new ParseObject("TestObject");
        testObject.put("foo", "bar");
        testObject.saveInBackground();*/

        SP = PreferenceManager.getDefaultSharedPreferences(this);

        //Abrimos la base de datos 'DBUsuarios' en modo escritura
        DBHelper msg_database = new DBHelper(this, "messages.db", null, 1);
        Global.db = msg_database.getWritableDatabase();

        enableBt();

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        setContentView(R.layout.activity_main);

        // Initilization
        viewPager = (ViewPager) findViewById(R.id.pager);
        actionbar = getActionBar();

        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        }
        checkUsername();

        TabsPagerAdapter mAdapter = new TabsPagerAdapter(getSupportFragmentManager());
        fm = getSupportFragmentManager();

        viewPager.setAdapter(mAdapter);
        actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionbar.addTab(actionbar.newTab().setText("General").setTabListener(this));
        actionbar.addTab(actionbar.newTab().setText("Tags").setTabListener(this));

        /**
         * on swiping the viewpager make respective tab selected
         * */
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            public void onPageSelected(int position) {
                // on changing the page
                // make respected tab selected
                actionbar.setSelectedNavigationItem(position);
            }

            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            public void onPageScrollStateChanged(int arg0) {
            }
        });

        /* EXPERIMENTAL SERVICES */

        //this.startService(new Intent(this, WifiService.class));
        //this.startService(new Intent(this, BtWizService.class));
        //this.startService(new Intent(this, MeshService.class));
        this.startService(new Intent(this, commService.class));
        //if (Global.mqtt) this.startService(new Intent(this, mqttService.class));
        //else this.startService(new Intent(this, BtService.class));
        //this.startService(new Intent(this, smoothService.class));

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(intent); // Handle text being sent
            } else if (type.startsWith("image/")) {
                handleSendImage(intent); // Handle single image being sent
            }

        }

    }

    void handleSendImage(Intent intent) {
        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            // Update UI to reflect image being shared
            try {

                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);

                int outWidth = 300;
                int outHeight = 300;

                if (bitmap.getWidth() > bitmap.getHeight()) {
                    outHeight = (bitmap.getHeight() * outWidth) / bitmap.getWidth();
                } else {
                    outWidth = (bitmap.getWidth() * outHeight) / bitmap.getHeight();
                }

                Bitmap resized_bm = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, true);

                String newPath = Environment.getExternalStorageDirectory().getPath() + "/floaty/";

                if (!isExternalStorageWritable()) {
                    newPath = Environment.getDataDirectory().getPath() + "/floaty/";
                }

                File directory = new File(newPath);
                directory.mkdirs();
                Log.w("MAIN", getRealPathFromURI(imageUri));
                File outputFile = new File(newPath, getFilenameFromURI(imageUri) + ".bmp");
                outputFile.createNewFile(); //added
                FileOutputStream out = new FileOutputStream(outputFile);
                resized_bm.compress(Bitmap.CompressFormat.PNG, 10, out);
                out.close();

                BtMessage item = new BtMessage(newPath + getFilenameFromURI(imageUri) + ".bmp", SP.getString("username_field", "anonymous"));
                item.isImage = true;

                DBHelper.insertMessage(Global.db, item);

                GeneralFragment.refresh();
                //Global.messages.add(item); // We add the message to the temporal list

            } catch (Exception e) {
                Log.e("Main", "Error retrieving image", e);
            }
        }

    }

    void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            BtMessage item = new BtMessage(sharedText, PreferenceManager.getDefaultSharedPreferences(this).getString("username_field", "anonymous"));
            DBHelper.insertMessage(Global.db, item);
            GeneralFragment.refresh();
            //Global.messages.add(item); // We add the message to the temporal list
        }
    }

    public void checkUsername() {
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(this);
        if (SP.getString("username_field", "anonymous").equals("anonymous")) {
            SP.edit().putString("username_field", android.os.Build.DEVICE).apply();

        }

    }

    public String getRealPathFromURI(Uri contentUri) {
        String path = null;
        String[] proj = {MediaStore.MediaColumns.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            path = cursor.getString(column_index);
        }
        cursor.close();
        return path;
    }


    public String getFilenameFromURI(Uri contentUri) {
        String path = getRealPathFromURI(contentUri);
        String filename = path.substring(path.lastIndexOf("/") + 1);
        filename = filename.substring(0, filename.lastIndexOf("."));
        return filename;
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /*
        Enables the BT and captures the MAC address to use it as a UUID
     */
    public void enableBt() {

        BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!mAdapter.isEnabled())
            mAdapter.enable();                                  //Turn On Bluetooth without Permission

        /*if (!mAdapter.isEnabled())
            startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));*/

        while (!mAdapter.isEnabled()) {
        }

        String restoredMAC = SP.getString("MAC", null);
        if (restoredMAC != null) {
            Global.DEVICE_ADDRESS = restoredMAC;
            if (restoredMAC.equals("00:00:00:00:00:00")) {
                Global.DEVICE_ADDRESS = mAdapter.getAddress();
                SharedPreferences.Editor editor = SP.edit();
                editor.putString("MAC", Global.DEVICE_ADDRESS);
                editor.apply();
            }
        } else {
            Global.DEVICE_ADDRESS = mAdapter.getAddress();
            SharedPreferences.Editor editor = SP.edit();
            editor.putString("MAC", Global.DEVICE_ADDRESS);
            editor.apply();
        }

        Log.w("MAC", Global.DEVICE_ADDRESS);

        if (mAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            startActivity(discoverableIntent);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        Bundle args;

        switch (item.getItemId()){

            case R.id.action_settings:
                try {
                    startActivity(new Intent(this, SettingsActivity.class));
                } catch (Exception e) {
                    Log.e("Main", "ERR", e);
                }
                return true;

            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;

            case R.id.action_newGrp:
                args = new Bundle();

                InputFragment newTagDialog = new InputFragment();
                newTagDialog.setArguments(args);
                newTagDialog.show(fm, "fragment_edit_name");

                return true;

            case R.id.action_exit:
                try {

                    setResult(RESULT_OK);
                    finish();
                    //android.os.Process.killProcess(android.os.Process.myPid());
                } catch (Exception e) {
                    Log.e("Main", "Error exiting", e);
                }
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(messageReceiver);

        } catch (Exception e) {

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //enableBt();
        checkUsername();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        //if (mChatService != null) {
        //    // Only if the state is STATE_NONE, do we know that we haven't started already
        //   if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
        //        // Start the Bluetooth chat services
        //        mChatService.start();
        //    }
        //}
        //if (mChatService == null) mChatService = new BluetoothChatService(this, mHandler);

        registerReceiver(messageReceiver, new IntentFilter("org.imdea.panel.MSG_RECEIVED"));
        registerReceiver(messageReceiver, new IntentFilter("org.imdea.panel.STATUS_CHANGED"));
    }

    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        viewPager.setCurrentItem(tab.getPosition());
    }

    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }

    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }

    protected void onDestroy() {
        super.onDestroy();
        if (Global.mqtt) {
            mqttService.disconnect();
            stopService(new Intent(this, mqttService.class));
        }

        stopService(new Intent(this, BtService.class));

        Global.db.close();
        Global.db = null;
        try {
            unregisterReceiver(messageReceiver);
        } catch (Exception e) {
            Log.e("Main", "Error unregistering messageReceiver");
        }
        BluetoothAdapter.getDefaultAdapter().disable();


    }

    public class newMessage extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equalsIgnoreCase("org.imdea.panel.MSG_RECEIVED")) {
                GeneralFragment.refresh();
                TagsFragment.refresh();
                //showMessages.refresh();
            }
            if (action.equalsIgnoreCase("org.imdea.panel.STATUS_CHANGED")) {
                String s = intent.getExtras().getString("STATUS");
                //setTitle(s);
                actionbar.setSubtitle(s);
            }
        }
    }

}


