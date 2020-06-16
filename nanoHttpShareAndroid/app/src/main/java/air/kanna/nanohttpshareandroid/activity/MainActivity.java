package air.kanna.nanohttpshareandroid.activity;

import android.os.Bundle;

import air.kanna.nanoHttpShare.ShareHttpService;
import air.kanna.nanohttpshareandroid.R;
import air.kanna.nanohttpshareandroid.activity.base.BasicActivity;

public class MainActivity extends BasicActivity {

    private ShareHttpService service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
