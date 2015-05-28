package org.imdea.panel;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import org.imdea.panel.Bluetooth.Global;
import org.imdea.panel.Database.BtMessage;
import org.imdea.panel.Database.BtNode;


public class InfoActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        String smac, suser, smsg, sdateime, stag, shits, speers;

        Bundle extras = getIntent().getExtras();
        suser = extras.getString("USER");
        smac = extras.getString("MAC");
        smsg = extras.getString("MSG");
        sdateime = extras.getString("DATETIME");
        stag = extras.getString("TAG");
        shits = "0";

        BtMessage item = new BtMessage(smac, smsg, suser);
        item.setTag(stag);
        speers = "";
        for (BtNode n : Global.nodes) {
            if (n.HasBeenReceived(item.toHash())) speers = speers + n.MAC + "\n";
        }

        for (BtMessage m : Global.messages) {
            if (m.toHash().equals(item.toHash())) shits = String.valueOf(m.hits);
        }


        TextView user, mesg, datetime, mac, peers, hits;

        user = (TextView) findViewById(R.id.fuser);
        mesg = (TextView) findViewById(R.id.fmesg);
        datetime = (TextView) findViewById(R.id.fdate);
        mac = (TextView) findViewById(R.id.fmac);
        peers = (TextView) findViewById(R.id.freceived);
        hits = (TextView) findViewById(R.id.fhits);

        hits.setText(shits);
        user.setText(suser);
        mesg.setText(smsg);
        datetime.setText(sdateime);
        mac.setText(smac);
        peers.setText(speers);

        hits.requestLayout();
        user.requestLayout();
        mesg.requestLayout();
        datetime.requestLayout();
        mac.requestLayout();
        peers.requestLayout();

    }


}
