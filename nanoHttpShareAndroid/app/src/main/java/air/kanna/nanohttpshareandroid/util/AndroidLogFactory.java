package air.kanna.nanohttpshareandroid.util;

import air.kanna.nanoHttpShare.logger.Logger;
import air.kanna.nanoHttpShare.logger.LoggerFactory;

public class AndroidLogFactory implements LoggerFactory {

    public Logger getLogger(Class cls){
        return new AndroidLogger(cls == null ? null : cls.getCanonicalName());
    }
    public Logger getLogger(String name){
        return new AndroidLogger(name);
    }
}
