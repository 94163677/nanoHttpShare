package air.kanna.nanohttpshareandroid.activity.base;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.Toast;

import air.kanna.nanoHttpShare.logger.LoggerProvider;
import air.kanna.nanohttpshareandroid.R;
import air.kanna.nanohttpshareandroid.util.AndroidLogFactory;

/**
 * Created on 2020-04-14.
 */

public class BasicActivity extends AppCompatActivity {
    protected static final int REQ_PERMISSION_STORAGE = 61531;

    protected boolean canBeBack = true;
    protected long prevTime = 0;

    protected ActionBar actionBar;
    protected Activity current;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //不显示标题栏
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //requestWindowFeature(Window.FEATURE_ACTION_BAR);
        //设置成竖屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setCanBeBack(true);
        ActivityManager.addActivity(this);
        actionBar = getSupportActionBar();
        current = this;

        //初始化日志
        try{
            LoggerProvider.getLogger("TEST");
        }catch (NullPointerException e) {
            LoggerProvider.resetLoggerFactory(new AndroidLogFactory());
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(!canBeBack && keyCode == KeyEvent.KEYCODE_BACK){
            long currTime = System.currentTimeMillis();
            if((currTime - prevTime) >= 1500){
                Toast.makeText(this, R.string.sys_exit_app_msg, Toast.LENGTH_SHORT).show();
            }else{
                ActivityManager.closeApp();
            }
            prevTime = currTime;
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy(){
        ActivityManager.removeActivity(this);

        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(canBeBack) {
            switch (item.getItemId()) {
                case android.R.id.home:
                    this.finish(); // back button
                    return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    protected void setCanBeBack(boolean canBeBack){
        this.canBeBack = canBeBack;
        if(actionBar != null){
            actionBar.setHomeButtonEnabled(canBeBack);
            actionBar.setDisplayHomeAsUpEnabled(canBeBack);
        }
    }

    protected void showErrorMessage(String msg, DialogInterface.OnClickListener listener){
        new AlertDialog.Builder(current)
                .setTitle(R.string.sys_error_title)
                .setMessage(msg)
                .setPositiveButton(R.string.sys_ok_button, listener)
                .show();
    }

    protected boolean checkStoragePermissionAndApply(String[] permissions, int requestCode){
        if(permissions == null || permissions.length <= 0){
            return true;
        }
        for(int i=0; i<permissions.length; i++){
            if(ContextCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED){
                requestPermission(permissions, requestCode);
                return false;
            }
        }
        return true;
    }

    protected void requestPermission(String[] permissions, int requestCode){
        if(permissions != null && permissions.length > 0){
            ActivityCompat.requestPermissions(this, permissions, requestCode);
        }
    }

}
