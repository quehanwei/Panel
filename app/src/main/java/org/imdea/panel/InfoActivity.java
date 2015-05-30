package org.imdea.panel;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.imdea.panel.Bluetooth.Global;
import org.imdea.panel.Database.BtMessage;
import org.imdea.panel.Database.Messages;

public class InfoActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        Bundle extras = getIntent().getExtras();
        String shash = extras.getString("HASH");

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(true);

        BtMessage item = Messages.getMessage(shash);

        if (item != null) {

            TextView user, mesg, orig_datetime, last_datetime, orig_mac, last_mac, peers, hits, hash;

            user = (TextView) findViewById(R.id.fuser);
            mesg = (TextView) findViewById(R.id.fmesg);
            last_datetime = (TextView) findViewById(R.id.last_timedate);
            orig_datetime = (TextView) findViewById(R.id.fdate);
            last_mac = (TextView) findViewById(R.id.last_mac);
            orig_mac = (TextView) findViewById(R.id.orig_mac);
            peers = (TextView) findViewById(R.id.freceived);
            hits = (TextView) findViewById(R.id.fhits);
            hash = (TextView) findViewById(R.id.fhash);

            hits.setText(String.valueOf(item.hits));
            user.setText(item.user);
            mesg.setText(item.msg);
            orig_datetime.setText(item.origin_date + " " + item.last_time);
            orig_mac.setText(item.origin_mac_address);
            peers.setText(item.devicesToString());
            hash.setText(item.toHash());

            if (item.origin_mac_address.contains(Global.DEVICE_ADDRESS)) {
                LinearLayout layout1 = (LinearLayout) findViewById(R.id.layo1);
                LinearLayout layout2 = (LinearLayout) findViewById(R.id.layo2);
                View view1 = findViewById(R.id.view1);
                layout1.setVisibility(View.GONE);
                layout2.setVisibility(View.GONE);
                view1.setVisibility(View.GONE);

            } else {
                last_datetime.setText(item.last_date + " " + item.last_time);
                last_mac.setText(item.last_mac_address);

            }

            hits.requestLayout();
            user.requestLayout();
            mesg.requestLayout();
            last_datetime.requestLayout();
            orig_datetime.requestLayout();
            last_mac.requestLayout();
            orig_mac.requestLayout();
            peers.requestLayout();
            hash.requestLayout();

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
