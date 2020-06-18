package air.kanna.nanohttpshareandroid.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import air.kanna.nanoHttpShare.ShareHttpService;
import air.kanna.nanoHttpShare.logger.Logger;
import air.kanna.nanoHttpShare.logger.LoggerProvider;
import air.kanna.nanoHttpShare.mapping.MappingFunction;
import air.kanna.nanoHttpShare.mapping.fileshare.FileShareFilterMapping;
import air.kanna.nanoHttpShare.mapping.texttrans.TextTransferFilterMapping;
import air.kanna.nanohttpshareandroid.R;
import air.kanna.nanohttpshareandroid.activity.base.ActivityManager;
import air.kanna.nanohttpshareandroid.activity.base.BasicActivity;
import air.kanna.nanohttpshareandroid.util.NetworkUtil;
import fi.iki.elonen.NanoHTTPD;

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
    private Button openBtn;

    private String ipAddr = null;
    private int port = 8090;

    private Logger logger;
    private ShareHttpService service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setCanBeBack(false);
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
        showQRCode();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        stopService();
    }

    private void init(){
        ipAddrTv = findViewById(R.id.ip_addr_tv);
        portTv = findViewById(R.id.port_tv);
        qrcodeIv = findViewById(R.id.qrcode_iv);
        startStopBtn = findViewById(R.id.start_stop_btn);
        retryBtn = findViewById(R.id.retry_btn);
        openBtn = findViewById(R.id.open_btn);

        initData();
        initControl();
    }

    private void initData(){
        port = getRandomPort();
        portTv.setText("" + port);

        checkAndGetIP();

        service = new ShareHttpService(port);
        /**应用程序的内外部路径
         * 内部：/storage/emulated/0/Android/data/air.kanna.nanohttpshareandroid/files/mounted
         * 外部：/storage/0000-0000/Android/data/air.kanna.nanohttpshareandroid/files/mounted
         * 所以后面要往上查找“Android”文件夹，它的父目录就是内外部的根目录
         * TODO 仅仅在三星手机上测试过，其他手机还不明确
         */
        File[] list = getExternalFilesDirs(Environment.MEDIA_MOUNTED);

        MappingFunction function = new MappingFunction(getString(R.string.sys_message_title), genUUID());
        TextTransferFilterMapping transfer = new TextTransferFilterMapping(function);
        service.addFilterMapping(transfer);

        for(File file : list) {
            if(file == null || !file.exists() || !file.isDirectory()){
                continue;
            }

            for(file = file.getParentFile(); !"Android".equals(file.getName()); file = file.getParentFile()){}
            file = file.getParentFile();

            String name = file.getAbsolutePath();
            if(name.contains("/emulated/")){
                name = getString(R.string.sys_internal_title);
            }else{
                name = getString(R.string.sys_external_title);
            }
            function = new MappingFunction(name, genUUID());
            FileShareFilterMapping mapping = new FileShareFilterMapping(file, function);
            mapping.setHomeString(getString(R.string.sys_root_title));
            mapping.setListTitle(getString(R.string.sys_file_list_title));

            service.addFilterMapping(mapping);
        }
    }

    private String genUUID(){
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    private String getShareUrl(){
        if(ipAddr == null){
            return null;
        }
        return "http://" + ipAddr + ":" + port;
    }

    private void showQRCode(){
        String url = getShareUrl();
        if(url == null){
            return;
        }
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        Map<EncodeHintType, String> param = new HashMap<>();
        Display defaultDisplay = getWindowManager().getDefaultDisplay();
        Point point = new Point();

        //获取屏幕宽度（反正是高宽最小的那个）的9成，小于400像素就用400像素
        defaultDisplay.getSize(point);
        int size = point.x > point.y ? point.y : point.x;
        size *= 0.9;
        if(size < 400){
            size = 400;
        }
        size = 400;

        param.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        param.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q.name());//纠错等级，从低到高为LMQH

        try{
            BitMatrix bitMatrix = multiFormatWriter.encode(url, BarcodeFormat.QR_CODE, size, size, param);
            Bitmap bitmap = getBitmapFromBitMatrix(bitMatrix);
            qrcodeIv.setImageBitmap(bitmap);
        }catch (Exception e){
            logger.error("Create QRCode error.", e);
            showErrorMessage(getString(R.string.sys_create_qrcode_error_msg, e.getMessage()), null);
        }
    }

    private Bitmap getBitmapFromBitMatrix(BitMatrix bitMatrix){
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();

        int[] pixels = new int[width * height];
        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                if(bitMatrix.get(x, y)){
                    pixels[y * width + x] = Color.BLACK;
                } else {
                    pixels[y * width + x] = Color.WHITE;
                }
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    private void initControl(){
        retryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndGetIP();
                showQRCode();
            }
        });
        startStopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(service.isAlive()){
                    stopService();
                }else{
                    startService();
                }
            }
        });
        openBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl();
            }
        });
    }

    private void openUrl(){
        String url = getShareUrl();
        if(url == null){
            return;
        }
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    private void startService(){
        if(service.isAlive()) {
            return;
        }
        try {
            service.start(NanoHTTPD.SOCKET_READ_TIMEOUT);
            startStopBtn.setText(getString(R.string.stop_service_btn));
            openBtn.setVisibility(View.VISIBLE);
            openBtn.setEnabled(true);
        }catch(Exception e) {
            logger.error("Start service error", e);
            showErrorMessage(getString(R.string.sys_start_service_error_msg, e.getMessage()), null);
        }
    }

    private void stopService(){
        if(!service.isAlive()) {
            return;
        }
        service.closeAllConnections();
        service.stop();
        startStopBtn.setText(R.string.start_service_btn);
        openBtn.setVisibility(View.GONE);
        openBtn.setEnabled(false);
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
        openBtn.setVisibility(View.GONE);
        openBtn.setEnabled(false);
        ipAddrTv.setText(ipAddr);
    }

    private void noNetwork(){
        ipAddr = null;
        retryBtn.setVisibility(View.VISIBLE);
        retryBtn.setEnabled(true);
        openBtn.setVisibility(View.GONE);
        openBtn.setEnabled(false);
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
                                    ActivityManager.closeApp();//不给权限则退出
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
