package air.kanna.nanohttpshareandroid.util;

import android.util.Log;

import air.kanna.nanoHttpShare.logger.Logger;
import air.kanna.nanohttpshareandroid.NanoHttpShareAndroid;


public class AndroidLogger implements Logger {
    private String myTag = "[]";

    AndroidLogger(String myTag){
        if(myTag == null || myTag.length() <= 0){

        }else{
            this.myTag = '[' + myTag + ']';
        }
    }

    public void debug(String msg){
        Log.d(NanoHttpShareAndroid.LOG_TAG, (myTag + msg));
    }

    public void info(String msg){
        Log.i(NanoHttpShareAndroid.LOG_TAG, (myTag + msg));
    }

    public void warn(String msg){
        Log.w(NanoHttpShareAndroid.LOG_TAG, (myTag + msg));
    }
    public void warn(String msg, Throwable tro){
        Log.w(NanoHttpShareAndroid.LOG_TAG, (myTag + msg), tro);
    }

    public void error(String msg){
        Log.e(NanoHttpShareAndroid.LOG_TAG, (myTag + msg));
    }
    public void error(String msg, Throwable tro){
        Log.e(NanoHttpShareAndroid.LOG_TAG, (myTag + msg), tro);
    }
}
