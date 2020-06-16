package air.kanna.nanohttpshareandroid.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import air.kanna.nanoHttpShare.ShareHttpService;
import air.kanna.nanoHttpShare.logger.Logger;
import air.kanna.nanoHttpShare.logger.LoggerProvider;
import air.kanna.nanohttpshareandroid.R;
import air.kanna.nanohttpshareandroid.activity.base.BasicActivity;
import air.kanna.nanohttpshareandroid.util.NetworkUtil;

public class MainActivity extends BasicActivity {
    private static final String[] STORAGE_PERMISSION = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
//            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private TextView ipAddrTv;
    private TextView portTv;
    private ImageView qrcodeIv;
    private Button startStopBtn;
    private Button retryBtn;

    private String ipAddr = null;
    private int port = 8090;

    private Logger logger;
    private ShareHttpService service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logger = LoggerProvider.getLogger(MainActivity.class);
        //检查并获取存储权限
        if(!checkStoragePermissionAndApply(STORAGE_PERMISSION, REQ_PERMISSION_STORAGE)){
            return;
        }
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void init(){
        ipAddrTv = findViewById(R.id.ip_addr_tv);
        portTv = findViewById(R.id.port_tv);
        qrcodeIv = findViewById(R.id.qrcode_iv);
        startStopBtn = findViewById(R.id.start_stop_btn);
        retryBtn = findViewById(R.id.retry_btn);

        initData();
        initControl();
    }

    private void initData(){
        port = getRandomPort();
        portTv.setText("" + port);

        checkAndGetIP();

    }

    private void initControl(){
        retryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndGetIP();
            }
        });
    }

    private int getRandomPort() {
        return (int)((Math.random() * 50000) + 10000);
    }

    private void checkAndGetIP(){
        if(!NetworkUtil.checkNetwork(current)){
            noNetwork();
            return;
        }
        String ipAddr = NetworkUtil.getIPAddress(current);
        if(ipAddr == null){
            noNetwork();
            return;
        }
        this.ipAddr = ipAddr;
        retryBtn.setVisibility(View.GONE);
        retryBtn.setEnabled(false);
        startStopBtn.setEnabled(true);
        ipAddrTv.setText(ipAddr);
    }

    private void noNetwork(){
        ipAddr = null;
        retryBtn.setVisibility(View.VISIBLE);
        retryBtn.setEnabled(true);
        startStopBtn.setEnabled(false);
        ipAddrTv.setText(R.string.sys_no_network);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            //外部存储的权限申请结果
            case REQ_PERMISSION_STORAGE: {
                boolean isDenied = false;
                for(int i=0; i<grantResults.length; i++){
                    if(PackageManager.PERMISSION_DENIED == grantResults[i]){
                        isDenied = true;
                        break;
                    }
                }
                //申请不到权限，再次询问，申请到则初始化
                if(isDenied){
                    new AlertDialog.Builder(current)
                            .setTitle(R.string.sys_error_title)
                            .setMessage(R.string.sys_apply_storage_msg)
                            .setPositiveButton(R.string.sys_ok_button, new DialogInterface.OnClickListener(){
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    checkStoragePermissionAndApply(STORAGE_PERMISSION, REQ_PERMISSION_STORAGE);//重新申请
                                }
                            })
                            .setNegativeButton(R.string.sys_cancel_button, new DialogInterface.OnClickListener(){
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();//不给权限则退出
                                }
                            })
                            .show();
                }else{
                    init();
                }
            };break;
            default: logger.warn("Cannot support requestCode: " + requestCode);
        }
    }
}
