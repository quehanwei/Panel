package org.imdea.panel;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebView;


public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(true);
        WebView myWebView = (WebView) this.findViewById(R.id.webView);
        myWebView.loadUrl("file:///android_asset/about.html");

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
